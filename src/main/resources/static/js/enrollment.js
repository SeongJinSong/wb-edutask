// DOM 요소
const studentSection = document.getElementById('studentSection');
const enrollmentSection = document.getElementById('enrollmentSection');
const studentForm = document.getElementById('studentForm');
const studentSubmitBtn = document.getElementById('studentSubmitBtn');
const studentInfoCard = document.getElementById('studentInfoCard');
const enrollmentsList = document.getElementById('enrollmentsList');
const loadingEnrollments = document.getElementById('loadingEnrollments');
const statusFilter = document.getElementById('statusFilter');
const searchInput = document.getElementById('searchInput');

// 모달 요소
const cancelConfirmModal = document.getElementById('cancelConfirmModal');
const messageModal = document.getElementById('messageModal');
const cancelMessage = document.getElementById('cancelMessage');
const modalTitle = document.getElementById('modalTitle');
const modalMessage = document.getElementById('modalMessage');

// 전역 변수
let currentStudent = null;
let studentEnrollments = [];
let currentStatusFilter = 'all';
let currentCancelEnrollmentId = null;

/**
 * 페이지 로드 시 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
});

/**
 * 이벤트 리스너 설정
 */
function setupEventListeners() {
    // 학생 정보 확인 폼
    studentForm.addEventListener('submit', handleStudentSubmit);
    
    // 필터링 및 검색
    statusFilter.addEventListener('change', handleStatusFilterChange);
    searchInput.addEventListener('input', handleSearch);
    
    // 모달 이벤트
    document.getElementById('closeCancelModal').addEventListener('click', () => hideModal(cancelConfirmModal));
    document.getElementById('cancelConfirmBtn').addEventListener('click', handleCancelConfirm);
    document.getElementById('cancelCancelBtn').addEventListener('click', () => hideModal(cancelConfirmModal));
    document.getElementById('closeModal').addEventListener('click', () => hideModal(messageModal));
    document.getElementById('modalOkBtn').addEventListener('click', () => hideModal(messageModal));
    
    // 모달 외부 클릭 시 닫기
    window.addEventListener('click', function(event) {
        if (event.target === cancelConfirmModal) {
            hideModal(cancelConfirmModal);
        }
        if (event.target === messageModal) {
            hideModal(messageModal);
        }
    });
}

/**
 * 학생 정보 확인 처리
 */
async function handleStudentSubmit(event) {
    event.preventDefault();
    
    const studentId = document.getElementById('studentId').value.trim();
    
    if (!studentId) {
        showMessageModal('오류', '학생 회원번호를 입력해주세요.', 'error');
        return;
    }
    
    try {
        setStudentLoading(true);
        
        // 학생 정보 조회
        const response = await fetch(`/api/v1/members/${studentId}`);
        
        if (response.ok) {
            const student = await response.json();
            
            if (student.memberType === 'STUDENT') {
                currentStudent = student;
                showStudentInfo(student);
                await loadStudentEnrollments();
                showEnrollmentSection();
            } else {
                showMessageModal('오류', '학생 권한이 없는 회원입니다.', 'error');
            }
        } else {
            showMessageModal('오류', '존재하지 않는 회원번호입니다.', 'error');
        }
        
    } catch (error) {
        console.error('학생 정보 조회 오류:', error);
        showMessageModal('오류', '학생 정보를 확인할 수 없습니다.', 'error');
    } finally {
        setStudentLoading(false);
    }
}

/**
 * 학생 정보 표시
 */
function showStudentInfo(student) {
    studentInfoCard.innerHTML = `
        <div class="student-info">
            <div class="info-header">
                <h3>👨‍🎓 ${escapeHtml(student.name)}</h3>
                <span class="student-badge">학생</span>
            </div>
            <div class="info-details">
                <div class="info-item">
                    <span class="label">회원번호:</span>
                    <span class="value">${student.id}</span>
                </div>
                <div class="info-item">
                    <span class="label">이메일:</span>
                    <span class="value">${escapeHtml(student.email)}</span>
                </div>
                <div class="info-item">
                    <span class="label">연락처:</span>
                    <span class="value">${escapeHtml(student.phoneNumber)}</span>
                </div>
            </div>
        </div>
    `;
}

/**
 * 수강신청 섹션 표시
 */
function showEnrollmentSection() {
    enrollmentSection.style.display = 'block';
    enrollmentSection.scrollIntoView({ behavior: 'smooth' });
}

/**
 * 학생의 수강신청 목록 로드
 */
async function loadStudentEnrollments() {
    try {
        showEnrollmentsLoading(true);
        
        const response = await fetch(`/api/v1/enrollments/student/${currentStudent.id}?size=100`);
        
        if (response.ok) {
            const data = await response.json();
            console.log('수강신청 목록 데이터:', data); // 디버깅용 로그
            studentEnrollments = data.content || [];
            displayEnrollments();
        } else {
            console.error('API 호출 실패:', response.status, response.statusText);
            const errorData = await response.text();
            console.error('오류 응답:', errorData);
            showMessageModal('오류', `수강신청 목록을 불러올 수 없습니다. (${response.status})`, 'error');
        }
        
    } catch (error) {
        console.error('수강신청 목록 로드 오류:', error);
        showMessageModal('오류', '수강신청 목록을 불러오는 중 오류가 발생했습니다.', 'error');
    } finally {
        showEnrollmentsLoading(false);
    }
}

/**
 * 수강신청 목록 표시
 */
function displayEnrollments() {
    let filteredEnrollments = [...studentEnrollments];
    
    // 상태 필터링
    if (currentStatusFilter !== 'all') {
        filteredEnrollments = filteredEnrollments.filter(enrollment => 
            enrollment.status === currentStatusFilter
        );
    }
    
    // 검색어 필터링
    const searchTerm = searchInput.value.trim().toLowerCase();
    if (searchTerm) {
        filteredEnrollments = filteredEnrollments.filter(enrollment => {
            const course = enrollment.course;
            const instructorName = (course.instructor && course.instructor.name) || course.instructorName || '';
            
            return course.courseName.toLowerCase().includes(searchTerm) ||
                   (course.description && course.description.toLowerCase().includes(searchTerm)) ||
                   instructorName.toLowerCase().includes(searchTerm);
        });
    }
    
    if (filteredEnrollments.length === 0) {
        enrollmentsList.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📋</div>
                <h3>신청한 강의가 없습니다</h3>
                <p>아직 수강신청한 강의가 없거나 검색 조건에 맞는 강의가 없습니다.</p>
            </div>
        `;
        return;
    }
    
    const enrollmentsHtml = filteredEnrollments.map(enrollment => {
        const course = enrollment.course;
        const canCancel = enrollment.status === 'APPROVED'; // 온라인 강의는 APPROVED 상태만 취소 가능
        
        return `
            <div class="enrollment-card" data-enrollment-id="${enrollment.id}">
                <div class="enrollment-header">
                    <h4 class="course-title">${escapeHtml(course.courseName)}</h4>
                    <div class="enrollment-status">
                        <span class="status-badge ${enrollment.status.toLowerCase()}">${getEnrollmentStatusText(enrollment.status)}</span>
                    </div>
                </div>
                
                <div class="course-info">
                    <div class="info-row">
                        <span class="label">강사:</span>
                        <span class="value">${escapeHtml((course.instructor && course.instructor.name) || course.instructorName || '미정')}</span>
                    </div>
                    <div class="info-row">
                        <span class="label">기간:</span>
                        <span class="value">${formatDate(course.startDate)} ~ ${formatDate(course.endDate)}</span>
                    </div>
                    <div class="info-row">
                        <span class="label">신청일:</span>
                        <span class="value">${formatDateTime(enrollment.appliedAt)}</span>
                    </div>
                    ${enrollment.status === 'APPROVED' ? `
                    <div class="info-row">
                        <span class="label">상태:</span>
                        <span class="value">승인됨</span>
                    </div>
                    ` : ''}
                </div>
                
                <div class="course-description">
                    <p>${escapeHtml(course.description)}</p>
                </div>
                
                <div class="enrollment-actions">
                    ${canCancel ? `
                        <button class="cancel-btn" onclick="showCancelConfirm(${enrollment.id}, '${escapeHtml(course.courseName)}')">
                            수강신청 취소
                        </button>
                    ` : `
                        <span class="cancel-disabled">취소 불가</span>
                    `}
                </div>
            </div>
        `;
    }).join('');
    
    enrollmentsList.innerHTML = enrollmentsHtml;
}

/**
 * 수강신청 취소 확인 모달 표시
 */
function showCancelConfirm(enrollmentId, courseName) {
    currentCancelEnrollmentId = enrollmentId;
    cancelMessage.innerHTML = `
        <p><strong>${escapeHtml(courseName)}</strong> 강의의 수강신청을 취소하시겠습니까?</p>
        <p class="warning-text">⚠️ 취소 후에는 다시 신청해야 합니다.</p>
    `;
    showModal(cancelConfirmModal);
}

/**
 * 수강신청 취소 확인 처리
 */
async function handleCancelConfirm() {
    if (!currentCancelEnrollmentId) return;
    
    try {
        hideModal(cancelConfirmModal);
        
        const response = await fetch(`/api/v1/enrollments/${currentCancelEnrollmentId}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            showMessageModal('성공', '수강신청이 취소되었습니다.', 'success');
            await loadStudentEnrollments(); // 목록 새로고침
        } else {
            const errorData = await response.json();
            showMessageModal('오류', errorData.message || '수강신청 취소에 실패했습니다.', 'error');
        }
        
    } catch (error) {
        console.error('수강신청 취소 오류:', error);
        showMessageModal('오류', '수강신청 취소 중 오류가 발생했습니다.', 'error');
    } finally {
        currentCancelEnrollmentId = null;
    }
}

/**
 * 상태 필터 변경 처리
 */
async function handleStatusFilterChange() {
    currentStatusFilter = statusFilter.value;
    displayEnrollments();
}

/**
 * 검색 처리
 */
function handleSearch() {
    displayEnrollments();
}

/**
 * 수강신청 상태 텍스트 변환
 */
function getEnrollmentStatusText(status) {
    switch (status) {
        case 'APPROVED': return '수강중';
        case 'CANCELLED': return '취소됨';
        default: return status;
    }
}

/**
 * 강의 상태 텍스트 변환
 */
function getStatusText(status) {
    switch (status) {
        case 'SCHEDULED': return '예정';
        case 'IN_PROGRESS': return '진행중';
        case 'COMPLETED': return '완료';
        case 'CANCELLED': return '취소';
        default: return status;
    }
}

/**
 * 날짜 포맷팅
 */
function formatDate(dateString) {
    if (!dateString) {
        return '미정';
    }
    
    try {
        const date = new Date(dateString);
        if (isNaN(date.getTime())) {
            return '미정';
        }
        return date.toLocaleDateString('ko-KR');
    } catch (error) {
        console.error('날짜 포맷팅 오류:', error);
        return '미정';
    }
}

/**
 * 날짜/시간 포맷팅
 */
function formatDateTime(dateTimeString) {
    if (!dateTimeString) {
        return '미정';
    }
    
    try {
        const date = new Date(dateTimeString);
        if (isNaN(date.getTime())) {
            return '미정';
        }
        return date.toLocaleString('ko-KR');
    } catch (error) {
        console.error('날짜/시간 포맷팅 오류:', error);
        return '미정';
    }
}

/**
 * 로딩 상태 설정
 */
function setStudentLoading(loading) {
    studentSubmitBtn.disabled = loading;
    const btnText = studentSubmitBtn.querySelector('.btn-text');
    const btnLoading = studentSubmitBtn.querySelector('.btn-loading');
    
    btnText.style.display = loading ? 'none' : 'inline';
    btnLoading.style.display = loading ? 'flex' : 'none';
}

function showEnrollmentsLoading(show) {
    loadingEnrollments.style.display = show ? 'flex' : 'none';
}

/**
 * 모달 표시/숨김
 */
function showModal(modal) {
    modal.style.display = 'flex';
}

function hideModal(modal) {
    modal.style.display = 'none';
}

/**
 * 메시지 모달 표시
 */
function showMessageModal(title, message, type = 'info') {
    modalTitle.textContent = title;
    modalMessage.textContent = message;
    
    const modalContent = messageModal.querySelector('.modal-content');
    modalContent.className = `modal-content ${type}`;
    
    showModal(messageModal);
}

/**
 * HTML 이스케이프
 */
function escapeHtml(text) {
    if (text === null || text === undefined) {
        return '';
    }
    
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return String(text).replace(/[&<>"']/g, function(m) { return map[m]; });
}
