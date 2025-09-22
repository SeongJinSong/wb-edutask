// 전역 변수
let allCourses = [];
let selectedCourses = new Set();
let currentSort = 'recent';
let currentPage = 0;
let totalPages = 0;
let isLoading = false;
let currentMember = null;

// DOM 요소
const courseList = document.getElementById('courseList');
const enrollBtn = document.getElementById('enrollBtn');
const loading = document.getElementById('loading');
const notification = document.getElementById('notification');
const notificationMessage = document.getElementById('notificationMessage');
const memberIdInput = document.getElementById('memberId');
const loginBtn = document.getElementById('loginBtn');
const memberInfo = document.getElementById('memberInfo');
const memberName = document.getElementById('memberName');
const memberType = document.getElementById('memberType');
const logoutBtn = document.getElementById('logoutBtn');
const selectedCount = document.getElementById('selectedCount');

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
    setupEventListeners();
});

/**
 * 앱 초기화
 */
function initializeApp() {
    updateEnrollButton(); // 초기 버튼 상태 설정
    loadCourses();
}

/**
 * 이벤트 리스너 설정
 */
function setupEventListeners() {
    // 정렬 옵션 변경 이벤트
    const sortRadios = document.querySelectorAll('input[name="sort"]');
    sortRadios.forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.checked) {
                changeSortAndReload(this.value);
            }
        });
    });

    // 회원 로그인 관련 이벤트
    loginBtn.addEventListener('click', handleLogin);
    logoutBtn.addEventListener('click', handleLogout);
    memberIdInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            handleLogin();
        }
    });

    // 수강 신청 버튼 클릭 이벤트
    enrollBtn.addEventListener('click', handleEnrollment);
}

/**
 * 강의 목록 로드 (서버 사이드 정렬 및 페이징)
 */
async function loadCourses(page = 0, append = false) {
    if (isLoading) return;
    
    try {
        isLoading = true;
        
        // 첫 페이지 로드시에만 로딩 스피너 표시
        if (!append || page === 0) {
            showLoading(true);
        }
        
        // 더보기 버튼 상태 업데이트 (로딩 중 표시)
        updateLoadMoreButton();
        
        // 서버 사이드 정렬을 위한 API 호출
        const response = await fetch(`/api/v1/courses/available?sortBy=${currentSort}&page=${page}&size=20`);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        const newCourses = data.content || [];
        
        // API에서 받은 실제 수강인원 데이터 처리
        processEnrollmentData(newCourses);
        
        if (append && page > 0) {
            // 더보기 기능: 기존 목록에 추가
            allCourses = [...allCourses, ...newCourses];
        } else {
            // 새로운 정렬이나 첫 로드: 목록 교체
            allCourses = newCourses;
        }
        
        currentPage = data.number;
        totalPages = data.totalPages;
        
        displayCourses(allCourses);
        updateLoadMoreButton();
        
    } catch (error) {
        console.error('강의 목록 로드 실패:', error);
        showNotification('강의 목록을 불러오는데 실패했습니다.', 'error');
        if (!append) displayEmptyState();
    } finally {
        isLoading = false;
        
        // 첫 페이지 로드시에만 로딩 스피너 숨김
        if (!append || page === 0) {
            showLoading(false);
        }
        
        // 더보기 버튼 최종 상태 업데이트
        updateLoadMoreButton();
    }
}

/**
 * 각 강의의 수강 신청자 수 정보 처리
 */
function processEnrollmentData(courses) {
    courses.forEach(course => {
        // API에서 받은 실제 데이터 사용
        course.currentStudents = course.currentEnrollments || 0;
        course.enrollmentRate = course.maxStudents > 0 ? 
            Math.round((course.currentStudents / course.maxStudents) * 100) : 0;
    });
}

/**
 * 정렬 변경 시 새로운 데이터 로드
 */
function changeSortAndReload(newSort) {
    if (currentSort !== newSort) {
        currentSort = newSort;
        currentPage = 0;
        selectedCourses.clear();
        updateEnrollButton();
        loadCourses(0, false); // 새로운 정렬로 첫 페이지 로드
    }
}

/**
 * 강의 목록 화면에 표시
 */
function displayCourses(courses) {
    if (courses.length === 0) {
        displayEmptyState();
        return;
    }
    
    courseList.innerHTML = courses.map(course => createCourseHTML(course)).join('');
    
    // 강의 카드 클릭 이벤트 추가 (체크박스 제외)
    const courseCards = document.querySelectorAll('.course-card');
    courseCards.forEach(card => {
        card.addEventListener('click', function(e) {
            // 체크박스 클릭은 제외
            if (e.target.type === 'checkbox') return;
            
            const courseId = parseInt(this.dataset.courseId);
            const checkbox = this.querySelector('.course-checkbox');
            
            // 체크박스 상태 토글
            checkbox.checked = !checkbox.checked;
            toggleCourseSelection(courseId, this);
        });
    });
    
    // 수강신청 버튼 상태 업데이트
    updateEnrollButton();
}

/**
 * 강의 HTML 생성
 */
function createCourseHTML(course) {
    const isSelected = selectedCourses.has(course.id);
    const startDate = new Date(course.startDate).toLocaleDateString('ko-KR');
    const endDate = new Date(course.endDate).toLocaleDateString('ko-KR');
    
    return `
        <div class="course-card ${isSelected ? 'selected' : ''}" data-course-id="${course.id}">
            <div class="course-header">
                <input type="checkbox" class="course-checkbox" ${isSelected ? 'checked' : ''} 
                       onchange="toggleCourseSelection(${course.id}, this.closest('.course-card'))">
                <h3 class="course-title">${escapeHtml(course.courseName)}</h3>
            </div>
            <div class="course-info">
                <div class="instructor-info">
                    <span class="instructor-name">👨‍🏫 ${escapeHtml(course.instructor?.name || '미정')}</span>
                </div>
                <div class="course-description">
                    ${escapeHtml(course.description || '강의 설명이 없습니다.')}
                </div>
                <div class="course-details">
                    <div class="detail-row">
                        <span class="label">📅 수강기간:</span>
                        <span class="value">${startDate} ~ ${endDate}</span>
                    </div>
                    <div class="detail-row">
                        <span class="label">👥 수강인원:</span>
                        <span class="value">${course.currentEnrollments || 0}/${course.maxStudents}명</span>
                    </div>
                    <div class="detail-row">
                        <span class="label">📊 수강률:</span>
                        <span class="value">${Math.round(((course.currentEnrollments || 0) / course.maxStudents) * 100)}%</span>
                    </div>
                    <div class="detail-row">
                        <span class="label">📋 상태:</span>
                        <span class="status-badge status-${course.status?.toLowerCase()}">${course.statusDescription || course.status}</span>
                    </div>
                </div>
            </div>
        </div>
    `;
}

/**
 * 강의 선택/해제 토글
 */
function toggleCourseSelection(courseId, element) {
    const checkbox = element.querySelector('.course-checkbox');
    
    if (selectedCourses.has(courseId)) {
        selectedCourses.delete(courseId);
        element.classList.remove('selected');
        if (checkbox) checkbox.checked = false;
    } else {
        selectedCourses.add(courseId);
        element.classList.add('selected');
        if (checkbox) checkbox.checked = true;
    }
    
    updateEnrollButton();
}

/**
 * 수강 신청 버튼 상태 업데이트
 */
function updateEnrollButton() {
    const hasSelection = selectedCourses.size > 0;
    enrollBtn.disabled = !hasSelection;
    
    if (hasSelection) {
        enrollBtn.textContent = `수강 신청하기 (${selectedCourses.size}개 강의)`;
    } else {
        enrollBtn.textContent = '수강 신청하기';
    }
}

/**
 * 수강 신청 처리
 */
async function handleEnrollment() {
    if (selectedCourses.size === 0) {
        showNotification('수강 신청할 강의를 선택해주세요.', 'warning');
        return;
    }
    
    try {
        showLoading(true);
        
        // 임시 학생 ID (실제로는 로그인 정보에서 가져와야 함)
        const studentId = 1;
        
        if (selectedCourses.size === 1) {
            // 단일 강의 수강 신청
            const courseId = Array.from(selectedCourses)[0];
            await enrollSingleCourse(studentId, courseId);
        } else {
            // 다중 강의 수강 신청
            await enrollMultipleCourses(studentId, Array.from(selectedCourses));
        }
        
        // 성공 시 선택 초기화 및 목록 새로고침
        selectedCourses.clear();
        updateEnrollButton();
        await loadCourses();
        
        showNotification('수강 신청이 완료되었습니다!', 'success');
        
    } catch (error) {
        console.error('수강 신청 실패:', error);
        showNotification('수강 신청에 실패했습니다. 다시 시도해주세요.', 'error');
    } finally {
        showLoading(false);
    }
}

/**
 * 단일 강의 수강 신청
 */
async function enrollSingleCourse(studentId, courseId) {
    const response = await fetch('/api/v1/enrollments', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            studentId: studentId,
            courseId: courseId
        })
    });
    
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
    }
    
    return await response.json();
}

/**
 * 다중 강의 수강 신청
 */
async function enrollMultipleCourses(studentId, courseIds) {
    const response = await fetch('/api/v1/enrollments/bulk', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            studentId: studentId,
            courseIds: courseIds
        })
    });
    
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
    }
    
    return await response.json();
}

/**
 * 빈 상태 표시
 */
function displayEmptyState() {
    courseList.innerHTML = `
        <div class="empty-state">
            <h3>수강 신청 가능한 강의가 없습니다</h3>
            <p>새로운 강의가 개설되면 알려드리겠습니다.</p>
        </div>
    `;
}

/**
 * 로딩 상태 표시/숨김
 */
function showLoading(show) {
    if (show) {
        loading.classList.remove('hidden');
    } else {
        loading.classList.add('hidden');
    }
}

/**
 * 알림 메시지 표시
 */
function showNotification(message, type = 'success') {
    notificationMessage.textContent = message;
    notification.className = `notification ${type} show`;
    
    // 3초 후 자동 숨김
    setTimeout(() => {
        hideNotification();
    }, 3000);
}

/**
 * 알림 메시지 숨김
 */
function hideNotification() {
    notification.classList.remove('show');
}

/**
 * 가격 포맷팅
 */
function formatPrice(price) {
    if (price === 0) {
        return '무료';
    }
    return price.toLocaleString('ko-KR') + '원';
}

/**
 * 더보기 버튼 상태 업데이트
 */
function updateLoadMoreButton() {
    let loadMoreBtn = document.getElementById('loadMoreBtn');
    
    if (!loadMoreBtn) {
        // 더보기 버튼이 없으면 생성
        loadMoreBtn = document.createElement('button');
        loadMoreBtn.id = 'loadMoreBtn';
        loadMoreBtn.className = 'load-more-btn';
        loadMoreBtn.textContent = '더보기';
        loadMoreBtn.addEventListener('click', loadMoreCourses);
        
        // 스타일 강제 적용
        loadMoreBtn.style.display = 'block';
        loadMoreBtn.style.background = '#007bff';
        loadMoreBtn.style.color = 'white';
        loadMoreBtn.style.border = 'none';
        loadMoreBtn.style.padding = '15px 40px';
        loadMoreBtn.style.borderRadius = '8px';
        loadMoreBtn.style.fontSize = '16px';
        loadMoreBtn.style.fontWeight = '600';
        loadMoreBtn.style.cursor = 'pointer';
        loadMoreBtn.style.margin = '30px auto 40px auto';
        loadMoreBtn.style.minWidth = '250px';
        loadMoreBtn.style.textAlign = 'center';
        loadMoreBtn.style.boxShadow = '0 2px 8px rgba(0, 123, 255, 0.2)';
        
        // 강의 목록 뒤에 삽입
        const courseList = document.querySelector('.course-list');
        const mainContent = document.querySelector('.main-content');
        
        if (courseList) {
            // 강의 목록 바로 뒤에 삽입
            courseList.parentNode.insertBefore(loadMoreBtn, courseList.nextSibling);
        } else if (mainContent) {
            // 대안: main 컨테이너에 삽입
            mainContent.appendChild(loadMoreBtn);
        }
    }
    
    // 더 로드할 페이지가 있는지 확인
    const hasMore = currentPage < totalPages - 1;
    loadMoreBtn.style.display = hasMore ? 'block' : 'none';
    loadMoreBtn.disabled = isLoading;
    
    // 스타일 강제 적용 (상태 변경 시에도)
    if (!isLoading && hasMore) {
        loadMoreBtn.style.background = '#007bff';
        loadMoreBtn.style.color = 'white';
        loadMoreBtn.style.cursor = 'pointer';
    } else if (isLoading) {
        loadMoreBtn.style.background = '#6c757d';
        loadMoreBtn.style.color = 'white';
        loadMoreBtn.style.cursor = 'not-allowed';
    }
    
    if (isLoading) {
        loadMoreBtn.textContent = '로딩 중...';
    } else if (hasMore) {
        const remainingPages = totalPages - currentPage - 1;
        const remainingCourses = remainingPages * 20; // 페이지당 20개
        loadMoreBtn.textContent = remainingPages > 0 ? 
            `더보기 (약 ${remainingCourses}개 강의 더 있음)` : 
            '더보기';
    }
}

/**
 * 더보기 버튼 클릭 시 다음 페이지 로드
 */
function loadMoreCourses() {
    if (currentPage < totalPages - 1) {
        loadCourses(currentPage + 1, true);
    }
}

/**
 * HTML 이스케이프
 */
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

/**
 * 회원 로그인 처리
 */
async function handleLogin() {
    const memberId = memberIdInput.value.trim();
    
    if (!memberId) {
        showNotification('회원번호를 입력해주세요.', 'error');
        return;
    }
    
    try {
        loginBtn.disabled = true;
        loginBtn.textContent = '로그인 중...';
        
        const response = await fetch(`/api/v1/members/${memberId}`);
        
        if (!response.ok) {
            if (response.status === 404) {
                throw new Error('존재하지 않는 회원번호입니다.');
            }
            throw new Error('회원 정보를 불러올 수 없습니다.');
        }
        
        const member = await response.json();
        currentMember = member;
        
        // UI 업데이트
        memberIdInput.style.display = 'none';
        loginBtn.style.display = 'none';
        memberInfo.style.display = 'flex';
        memberName.textContent = `${member.name}님`;
        memberType.textContent = member.memberType === 'STUDENT' ? '학생' : '강사';
        
        updateEnrollButton();
        showNotification(`${member.name}님 환영합니다!`, 'success');
        
    } catch (error) {
        console.error('로그인 실패:', error);
        showNotification(error.message, 'error');
    } finally {
        loginBtn.disabled = false;
        loginBtn.textContent = '로그인';
    }
}

/**
 * 로그아웃 처리
 */
function handleLogout() {
    currentMember = null;
    selectedCourses.clear();
    
    // UI 업데이트
    memberIdInput.style.display = 'block';
    memberIdInput.value = '';
    loginBtn.style.display = 'block';
    memberInfo.style.display = 'none';
    
    // 선택된 강의 초기화
    document.querySelectorAll('.course-card.selected').forEach(card => {
        card.classList.remove('selected');
        const checkbox = card.querySelector('.course-checkbox');
        if (checkbox) checkbox.checked = false;
    });
    
    updateEnrollButton();
    showNotification('로그아웃되었습니다.', 'info');
}

/**
 * 수강 신청 버튼 상태 업데이트
 */
function updateEnrollButton() {
    const hasSelection = selectedCourses.size > 0;
    const hasLogin = currentMember !== null;
    
    enrollBtn.disabled = !hasSelection || !hasLogin;
    
    if (!hasLogin) {
        enrollBtn.textContent = '로그인 후 수강신청 가능';
    } else if (hasSelection) {
        enrollBtn.textContent = `선택한 강의 수강신청하기 (${selectedCourses.size}개)`;
    } else {
        enrollBtn.textContent = '강의를 선택해주세요';
    }
    
    // 선택된 강의 수 업데이트
    selectedCount.textContent = `선택된 강의: ${selectedCourses.size}개`;
}

/**
 * 수강신청 처리
 */
async function handleEnrollment() {
    if (!currentMember) {
        showNotification('로그인이 필요합니다.', 'error');
        return;
    }
    
    if (selectedCourses.size === 0) {
        showNotification('수강신청할 강의를 선택해주세요.', 'error');
        return;
    }
    
    if (currentMember.memberType !== 'STUDENT') {
        showNotification('학생만 수강신청이 가능합니다.', 'error');
        return;
    }
    
    try {
        enrollBtn.disabled = true;
        enrollBtn.textContent = '수강신청 처리 중...';
        
        const courseIds = Array.from(selectedCourses);
        const enrollmentData = {
            studentId: currentMember.id,
            courseIds: courseIds
        };
        
        const response = await fetch('/api/v1/enrollments/bulk', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(enrollmentData)
        });
        
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || '수강신청에 실패했습니다.');
        }
        
        const result = await response.json();
        
        // 성공한 강의들 선택 해제
        selectedCourses.clear();
        document.querySelectorAll('.course-card.selected').forEach(card => {
            card.classList.remove('selected');
            const checkbox = card.querySelector('.course-checkbox');
            if (checkbox) checkbox.checked = false;
        });
        
        updateEnrollButton();
        
        // 강의 목록 새로고침 (수강인원 업데이트)
        loadCourses(0, false);
        
        showNotification(`${courseIds.length}개 강의 수강신청이 완료되었습니다!`, 'success');
        
    } catch (error) {
        console.error('수강신청 실패:', error);
        showNotification(error.message, 'error');
    } finally {
        enrollBtn.disabled = false;
        updateEnrollButton();
    }
}
