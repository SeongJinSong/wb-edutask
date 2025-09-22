// DOM 요소
const signupForm = document.getElementById('signupForm');
const submitBtn = document.getElementById('submitBtn');
const btnText = document.querySelector('.btn-text');
const btnLoading = document.querySelector('.btn-loading');
const messageModal = document.getElementById('messageModal');
const modalTitle = document.getElementById('modalTitle');
const modalMessage = document.getElementById('modalMessage');
const closeModal = document.getElementById('closeModal');
const modalOkBtn = document.getElementById('modalOkBtn');

/**
 * 페이지 로드 시 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
    setupEventListeners();
    setupValidation();
});

/**
 * 이벤트 리스너 설정
 */
function setupEventListeners() {
    // 폼 제출 이벤트
    signupForm.addEventListener('submit', handleSignup);
    
    // 모달 닫기 이벤트
    closeModal.addEventListener('click', hideModal);
    modalOkBtn.addEventListener('click', hideModal);
    
    // 모달 외부 클릭 시 닫기
    window.addEventListener('click', function(event) {
        if (event.target === messageModal) {
            hideModal();
        }
    });
    
    // 비밀번호 확인 실시간 검증
    const password = document.getElementById('password');
    const confirmPassword = document.getElementById('confirmPassword');
    
    confirmPassword.addEventListener('input', function() {
        validatePasswordMatch();
    });
    
    password.addEventListener('input', function() {
        validatePasswordMatch();
        validatePasswordStrength();
    });
    
    // 휴대폰 번호 자동 포맷팅
    const phoneInput = document.getElementById('phoneNumber');
    phoneInput.addEventListener('input', formatPhoneNumber);
}

/**
 * 폼 유효성 검사 설정
 */
function setupValidation() {
    const inputs = document.querySelectorAll('.form-input');
    
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
 * 회원가입 처리
 */
async function handleSignup(event) {
    event.preventDefault();
    
    // 폼 유효성 검사
    if (!validateForm()) {
        return;
    }
    
    const formData = new FormData(signupForm);
    const memberData = {
        name: formData.get('name').trim(),
        email: formData.get('email').trim(),
        phoneNumber: formData.get('phoneNumber').trim(),
        password: formData.get('password'),
        memberType: formData.get('memberType')
    };
    
    try {
        setLoading(true);
        
        // API 호출
        const response = await fetch('/api/v1/members/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(memberData)
        });
        
        const result = await response.json();
        
        if (response.ok) {
            showModal('가입 완료', '회원가입이 성공적으로 완료되었습니다!', 'success');
            signupForm.reset();
        } else {
            showModal('가입 실패', result.message || '회원가입 중 오류가 발생했습니다.', 'error');
        }
        
    } catch (error) {
        console.error('회원가입 오류:', error);
        showModal('가입 실패', '서버 연결에 실패했습니다. 잠시 후 다시 시도해주세요.', 'error');
    } finally {
        setLoading(false);
    }
}

/**
 * 폼 전체 유효성 검사
 */
function validateForm() {
    const inputs = document.querySelectorAll('.form-input[required]');
    let isValid = true;
    
    inputs.forEach(input => {
        if (!validateField(input)) {
            isValid = false;
        }
    });
    
    // 비밀번호 일치 확인
    if (!validatePasswordMatch()) {
        isValid = false;
    }
    
    // 비밀번호 강도 확인
    if (!validatePasswordStrength()) {
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
        case 'email':
            if (value && !isValidEmail(value)) {
                showFieldError(input, '올바른 이메일 형식을 입력해주세요.');
                return false;
            }
            break;
            
        case 'phoneNumber':
            if (value && !isValidPhoneNumber(value)) {
                showFieldError(input, '올바른 휴대폰 번호를 입력해주세요. (예: 010-1234-5678)');
                return false;
            }
            break;
            
        case 'name':
            if (value && (value.length < 2 || value.length > 50)) {
                showFieldError(input, '이름은 2자 이상 50자 이하로 입력해주세요.');
                return false;
            }
            break;
    }
    
    clearFieldError(input);
    return true;
}

/**
 * 비밀번호 일치 확인
 */
function validatePasswordMatch() {
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const confirmInput = document.getElementById('confirmPassword');
    
    if (confirmPassword && password !== confirmPassword) {
        showFieldError(confirmInput, '비밀번호가 일치하지 않습니다.');
        return false;
    }
    
    if (confirmPassword && password === confirmPassword) {
        clearFieldError(confirmInput);
    }
    
    return true;
}

/**
 * 비밀번호 강도 검사
 */
function validatePasswordStrength() {
    const password = document.getElementById('password').value;
    const passwordInput = document.getElementById('password');
    
    if (password && !isValidPassword(password)) {
        showFieldError(passwordInput, '비밀번호는 영문 대소문자, 숫자를 포함해야 합니다.');
        return false;
    }
    
    if (password && isValidPassword(password)) {
        clearFieldError(passwordInput);
    }
    
    return true;
}

/**
 * 휴대폰 번호 자동 포맷팅
 */
function formatPhoneNumber(event) {
    let value = event.target.value.replace(/[^\d]/g, '');
    
    if (value.length >= 11) {
        value = value.substring(0, 11);
    }
    
    if (value.length > 6) {
        value = value.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
    } else if (value.length > 3) {
        value = value.replace(/(\d{3})(\d+)/, '$1-$2');
    }
    
    event.target.value = value;
}

/**
 * 이메일 유효성 검사
 */
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

/**
 * 휴대폰 번호 유효성 검사
 */
function isValidPhoneNumber(phone) {
    const phoneRegex = /^010-\d{4}-\d{4}$/;
    return phoneRegex.test(phone);
}

/**
 * 비밀번호 유효성 검사
 */
function isValidPassword(password) {
    // 영문 대소문자, 숫자 포함, 최소 6자
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{6,}$/;
    return passwordRegex.test(password);
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
function showModal(title, message, type = 'info') {
    modalTitle.textContent = title;
    modalMessage.textContent = message;
    
    // 모달 타입에 따른 스타일 적용
    const modalContent = messageModal.querySelector('.modal-content');
    modalContent.className = `modal-content ${type}`;
    
    messageModal.style.display = 'flex';
}

/**
 * 모달 숨김
 */
function hideModal() {
    messageModal.style.display = 'none';
}
