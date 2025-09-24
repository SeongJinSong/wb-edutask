// DOM 요소
const courseForm = document.getElementById('courseForm');
const submitBtn = document.getElementById('submitBtn');
const cancelBtn = document.getElementById('cancelBtn');
const btnText = document.querySelector('.btn-text');
const btnLoading = document.querySelector('.btn-loading');

// 모달 요소
const instructorModal = document.getElementById('instructorModal');
const messageModal = document.getElementById('messageModal');
const instructorInfo = document.getElementById('instructorInfo');
const modalTitle = document.getElementById('modalTitle');
const modalMessage = document.getElementById('modalMessage');

// 버튼 요소
const closeInstructorModal = document.getElementById('closeInstructorModal');
const confirmInstructor = document.getElementById('confirmInstructor');
const cancelInstructor = document.getElementById('cancelInstructor');
const closeModal = document.getElementById('closeModal');
const modalOkBtn = document.getElementById('modalOkBtn');

// 전역 변수
let currentInstructor = null;

/**
 * 페이지 로드 시 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
    setupValidation();
    setDefaultDates();
});

/**
 * 이벤트 리스너 설정
 */
function setupEventListeners() {
    // 폼 제출 이벤트
    courseForm.addEventListener('submit', handleCourseSubmit);
    
    // 취소 버튼
    cancelBtn.addEventListener('click', handleCancel);
    
    // 강사 번호 입력 시 검증
    const instructorIdInput = document.getElementById('instructorId');
    instructorIdInput.addEventListener('blur', validateInstructor);
    
    // 가격 입력 - 숫자만 허용 (포맷팅 제거)
    const priceInput = document.getElementById('price');
    // formatPrice 이벤트 리스너 제거 - type="number"와 충돌 방지
    
    // 날짜 유효성 검사
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    
    startDateInput.addEventListener('change', validateDates);
    endDateInput.addEventListener('change', validateDates);
    
    // 모달 이벤트
    closeInstructorModal.addEventListener('click', () => hideModal(instructorModal));
    confirmInstructor.addEventListener('click', handleInstructorConfirm);
    cancelInstructor.addEventListener('click', () => hideModal(instructorModal));
    
    closeModal.addEventListener('click', () => hideModal(messageModal));
    modalOkBtn.addEventListener('click', () => hideModal(messageModal));
    
    // 모달 외부 클릭 시 닫기
    window.addEventListener('click', function(event) {
        if (event.target === instructorModal) {
            hideModal(instructorModal);
        }
        if (event.target === messageModal) {
            hideModal(messageModal);
        }
    });
}

/**
 * 폼 유효성 검사 설정
 */
function setupValidation() {
    const inputs = document.querySelectorAll('.form-input, .form-textarea');
    
    inputs.forEach(input => {
        input.addEventListener('blur', function() {
            validateField(this);
        });
        
        input.addEventListener('input', function() {
            clearFieldError(this);
        });
    });
}

/**
 * 기본 날짜 설정 (오늘부터 30일 후)
 */
function setDefaultDates() {
    const today = new Date();
    const startDate = new Date(today);
    startDate.setDate(today.getDate() + 7); // 일주일 후
    
    const endDate = new Date(startDate);
    endDate.setDate(startDate.getDate() + 30); // 시작일로부터 30일 후
    
    document.getElementById('startDate').value = formatDateForInput(startDate);
    document.getElementById('endDate').value = formatDateForInput(endDate);
}

/**
 * 날짜를 input[type="date"] 형식으로 변환
 */
function formatDateForInput(date) {
    return date.toISOString().split('T')[0];
}

/**
 * 강사 정보 검증
 */
async function validateInstructor() {
    const instructorId = document.getElementById('instructorId').value.trim();
    
    if (!instructorId) {
        return;
    }
    
    try {
        const response = await fetch(`/api/v1/members/${instructorId}`);
        
        if (response.ok) {
            const instructor = await response.json();
            
            if (instructor.memberType === 'INSTRUCTOR') {
                currentInstructor = instructor;
                showInstructorInfo(instructor);
                clearFieldError(document.getElementById('instructorId'));
            } else {
                showFieldError(document.getElementById('instructorId'), '강사 권한이 없는 회원입니다.');
                currentInstructor = null;
            }
        } else {
            showFieldError(document.getElementById('instructorId'), '존재하지 않는 회원번호입니다.');
            currentInstructor = null;
        }
    } catch (error) {
        console.error('강사 정보 조회 오류:', error);
        showFieldError(document.getElementById('instructorId'), '강사 정보를 확인할 수 없습니다.');
        currentInstructor = null;
    }
}

/**
 * 강사 정보 표시
 */
function showInstructorInfo(instructor) {
    instructorInfo.innerHTML = `
        <div class="instructor-card">
            <h4>강사 정보</h4>
            <div class="info-row">
                <span class="label">이름:</span>
                <span class="value">${escapeHtml(instructor.name)}</span>
            </div>
            <div class="info-row">
                <span class="label">이메일:</span>
                <span class="value">${escapeHtml(instructor.email)}</span>
            </div>
            <div class="info-row">
                <span class="label">회원번호:</span>
                <span class="value">${instructor.id}</span>
            </div>
            <p class="confirm-text">이 강사로 강의를 등록하시겠습니까?</p>
        </div>
    `;
    
    showModal(instructorModal);
}

/**
 * 강사 확인 처리
 */
function handleInstructorConfirm() {
    hideModal(instructorModal);
    // 강사가 확인되면 다음 필드로 포커스 이동
    document.getElementById('courseName').focus();
}

/**
 * 가격 포맷팅 (사용 안함 - type="number"와 충돌)
 * HTML input type="number"는 콤마가 포함된 값을 허용하지 않음
 */
/*
function formatPrice(event) {
    let value = event.target.value.replace(/[^\d]/g, '');
    
    if (value) {
        // 천 단위 콤마 추가
        value = parseInt(value).toLocaleString('ko-KR');
    }
    
    event.target.value = value;
}
*/

/**
 * 날짜 유효성 검사
 */
function validateDates() {
    const startDate = new Date(document.getElementById('startDate').value);
    const endDate = new Date(document.getElementById('endDate').value);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    const startInput = document.getElementById('startDate');
    const endInput = document.getElementById('endDate');
    
    // 시작일이 오늘 이후인지 확인
    if (startDate < today) {
        showFieldError(startInput, '시작일은 오늘 이후여야 합니다.');
        return false;
    }
    
    // 종료일이 시작일 이후인지 확인
    if (endDate <= startDate) {
        showFieldError(endInput, '종료일은 시작일 이후여야 합니다.');
        return false;
    }
    
    clearFieldError(startInput);
    clearFieldError(endInput);
    return true;
}

/**
 * 강의 등록 처리
 */
async function handleCourseSubmit(event) {
    event.preventDefault();
    
    // 폼 유효성 검사
    if (!validateForm()) {
        return;
    }
    
    // 강사 정보 확인
    if (!currentInstructor) {
        showMessageModal('오류', '강사 정보를 먼저 확인해주세요.', 'error');
        return;
    }
    
    const formData = new FormData(courseForm);
    const courseData = {
        courseName: formData.get('courseName').trim(),
        description: formData.get('description').trim(),
        instructorId: parseInt(formData.get('instructorId')),
        maxStudents: parseInt(formData.get('maxStudents')),
        price: parseInt(formData.get('price')) || 0, // 가격 필드 추가 (빈 값이면 0으로 설정)
        startDate: formData.get('startDate'),
        endDate: formData.get('endDate')
    };
    
    try {
        setLoading(true);
        
        // API 호출
        const response = await fetch('/api/v1/courses', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(courseData)
        });
        
        const result = await response.json();
        
        if (response.ok) {
            showMessageModal('등록 완료', '강의가 성공적으로 등록되었습니다!', 'success');
            courseForm.reset();
            currentInstructor = null;
            setDefaultDates();
        } else {
            showMessageModal('등록 실패', result.message || '강의 등록 중 오류가 발생했습니다.', 'error');
        }
        
    } catch (error) {
        console.error('강의 등록 오류:', error);
        showMessageModal('등록 실패', '서버 연결에 실패했습니다. 잠시 후 다시 시도해주세요.', 'error');
    } finally {
        setLoading(false);
    }
}

/**
 * 취소 처리
 */
function handleCancel() {
    if (confirm('작성 중인 내용이 모두 사라집니다. 정말 취소하시겠습니까?')) {
        courseForm.reset();
        currentInstructor = null;
        setDefaultDates();
        window.location.href = '/';
    }
}

/**
 * 폼 전체 유효성 검사
 */
function validateForm() {
    const inputs = document.querySelectorAll('.form-input[required], .form-textarea[required]');
    let isValid = true;
    
    inputs.forEach(input => {
        if (!validateField(input)) {
            isValid = false;
        }
    });
    
    // 날짜 유효성 검사
    if (!validateDates()) {
        isValid = false;
    }
    
    return isValid;
}

/**
 * 개별 필드 유효성 검사
 */
function validateField(input) {
    const value = input.value.trim();
    const fieldName = input.name;
    
    // 필수 필드 검사
    if (input.hasAttribute('required') && !value) {
        showFieldError(input, '이 필드는 필수입니다.');
        return false;
    }
    
    // 필드별 특별 검사
    switch (fieldName) {
        case 'courseName':
            if (value && (value.length < 2 || value.length > 100)) {
                showFieldError(input, '강의명은 2자 이상 100자 이하로 입력해주세요.');
                return false;
            }
            break;
            
        case 'description':
            if (value && value.length > 1000) {
                showFieldError(input, '강의 설명은 1000자 이하로 입력해주세요.');
                return false;
            }
            break;
            
        case 'maxStudents':
            const maxStudents = parseInt(value);
            if (value && (maxStudents < 1 || maxStudents > 100)) {
                showFieldError(input, '수강 인원은 1명 이상 100명 이하로 입력해주세요.');
                return false;
            }
            break;
            
        case 'price':
            const price = parseInt(value);
            if (value && (price < 0 || price > 10000000)) {
                showFieldError(input, '가격은 0원 이상 1,000만원 이하로 입력해주세요.');
                return false;
            }
            break;
    }
    
    clearFieldError(input);
    return true;
}

/**
 * 필드 오류 표시
 */
function showFieldError(input, message) {
    clearFieldError(input);
    
    input.classList.add('error');
    
    const errorDiv = document.createElement('div');
    errorDiv.className = 'field-error';
    errorDiv.textContent = message;
    
    input.parentNode.appendChild(errorDiv);
}

/**
 * 필드 오류 제거
 */
function clearFieldError(input) {
    input.classList.remove('error');
    
    const existingError = input.parentNode.querySelector('.field-error');
    if (existingError) {
        existingError.remove();
    }
}

/**
 * 로딩 상태 설정
 */
function setLoading(loading) {
    submitBtn.disabled = loading;
    btnText.style.display = loading ? 'none' : 'inline';
    btnLoading.style.display = loading ? 'flex' : 'none';
}

/**
 * 모달 표시
 */
function showModal(modal) {
    modal.style.display = 'flex';
}

/**
 * 모달 숨김
 */
function hideModal(modal) {
    modal.style.display = 'none';
}

/**
 * 메시지 모달 표시
 */
function showMessageModal(title, message, type = 'info') {
    modalTitle.textContent = title;
    modalMessage.textContent = message;
    
    // 모달 타입에 따른 스타일 적용
    const modalContent = messageModal.querySelector('.modal-content');
    modalContent.className = `modal-content ${type}`;
    
    showModal(messageModal);
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
