// 전역 변수
let allMembers = [];
let currentMemberType = 'all';
let currentPage = 0;
let totalPages = 0;
let isLastPage = false;
let isLoading = false;

// DOM 요소
let membersList, loadingMembers, searchInput, searchBtn, memberModal;

/**
 * DOM 요소 초기화
 */
function initializeDOMElements() {
    membersList = document.getElementById('membersList');
    loadingMembers = document.getElementById('loadingMembers');
    searchInput = document.getElementById('searchInput');
    searchBtn = document.getElementById('searchBtn');
    memberModal = document.getElementById('memberModal');
    
    // DOM 요소 존재 확인
    console.log('DOM 요소 확인:', {
        membersList: !!membersList,
        loadingMembers: !!loadingMembers,
        searchInput: !!searchInput,
        searchBtn: !!searchBtn,
        memberModal: !!memberModal
    });
    
    if (!membersList) {
        console.error('membersList 요소를 찾을 수 없습니다!');
        return false;
    }
    
    return true;
}

/**
 * 페이지 로드 시 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
    setupEventListeners();
});

/**
 * 앱 초기화 (강의목록과 동일한 방식)
 */
function initializeApp() {
    console.log('회원관리 앱 초기화 시작');
    
    // DOM 요소 초기화
    if (!initializeDOMElements()) {
        console.error('DOM 요소 초기화 실패');
        return;
    }
    
    // 회원 목록 로드
    loadMembers();
}

/**
 * 이벤트 리스너 설정
 */
function setupEventListeners() {
    // 회원 유형 필터 변경 이벤트
    const memberTypeRadios = document.querySelectorAll('input[name="memberType"]');
    memberTypeRadios.forEach(radio => {
        radio.addEventListener('change', function() {
            if (this.checked) {
                currentMemberType = this.value;
                console.log('회원 유형 변경:', currentMemberType);
                filterAndDisplayMembers();
            }
        });
    });

    // 검색 버튼 클릭 이벤트
    if (searchBtn) {
        searchBtn.addEventListener('click', handleSearch);
        console.log('검색 버튼 이벤트 리스너 등록 완료');
    }
    
    // 검색 입력 엔터키 이벤트
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                handleSearch();
            }
        });
        console.log('검색 입력 이벤트 리스너 등록 완료');
    }

    // 모달 닫기 이벤트
    const closeModal = document.querySelector('.close');
    if (closeModal) {
        closeModal.addEventListener('click', function() {
            memberModal.style.display = 'none';
        });
    }

    // 모달 외부 클릭 시 닫기
    window.addEventListener('click', function(event) {
        if (event.target === memberModal) {
            memberModal.style.display = 'none';
        }
    });
}

/**
 * 회원 목록 로드 (API 호출 - 페이징)
 */
async function loadMembers(page = 0, append = false) {
    if (isLoading) return;
    
    try {
        isLoading = true;
        showLoading(true);
        
        // API에서 회원 목록 가져오기 (페이징)
        const response = await fetch(`/api/v1/members?page=${page}&size=20&sort=createdAt,desc`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        // 페이징된 응답 처리
        if (data.content && Array.isArray(data.content)) {
            if (append) {
                allMembers = [...allMembers, ...data.content];
            } else {
                allMembers = data.content;
            }
            
            currentPage = data.number;
            totalPages = data.totalPages;
            isLastPage = data.last;
            
            console.log(`회원 목록 로드 완료: 페이지 ${currentPage + 1}/${totalPages}, 총 ${data.totalElements}명, isLastPage: ${isLastPage}`);
        } else {
            throw new Error('잘못된 응답 형식입니다.');
        }
        
        displayMembers(allMembers);
        updateLoadMoreButton();
        
    } catch (error) {
        console.error('회원 목록 로드 실패:', error);
        
        // 서버 연결 실패 시 특별한 메시지
        if (error.message.includes('fetch') || error.message.includes('Failed to fetch')) {
            showNotification('서버에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.', 'error');
        } else {
            showNotification('회원 목록을 불러오는데 실패했습니다: ' + error.message, 'error');
        }
        
        // API 실패 시 빈 상태 표시
        if (!append) {
            allMembers = [];
            displayEmptyStateWithError();
        }
        
        // 에러 시에도 페이징 상태 초기화
        currentPage = 0;
        totalPages = 1;
        isLastPage = true;
    } finally {
        isLoading = false;
        showLoading(false);
        updateLoadMoreButton(); // 에러 시에도 버튼 상태 업데이트
    }
}

/**
 * 더보기 버튼 상태 업데이트 (강의목록과 동일한 방식)
 */
function updateLoadMoreButton() {
    let loadMoreBtn = document.getElementById('loadMoreMembersBtn');
    
    if (!loadMoreBtn) {
        // 더보기 버튼이 없으면 생성
        loadMoreBtn = document.createElement('button');
        loadMoreBtn.id = 'loadMoreMembersBtn';
        loadMoreBtn.className = 'load-more-btn';
        loadMoreBtn.textContent = '더보기';
        loadMoreBtn.addEventListener('click', loadMoreMembers);
        
        // 스타일 강제 적용 (강의목록과 동일)
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
        
        // 회원 테이블 뒤에 삽입
        const tableContainer = document.querySelector('.table-container');
        const membersSection = document.querySelector('.members-section');
        
        if (tableContainer) {
            // 테이블 컨테이너 바로 뒤에 삽입
            tableContainer.parentNode.insertBefore(loadMoreBtn, tableContainer.nextSibling);
        } else if (membersSection) {
            // 대안: members-section에 삽입
            membersSection.appendChild(loadMoreBtn);
        }
    }
    
    // 더 로드할 페이지가 있는지 확인
    const hasMore = currentPage < totalPages - 1;
    
    // 버튼은 항상 표시 (숨기지 않음)
    loadMoreBtn.style.display = 'block';
    loadMoreBtn.disabled = isLoading || !hasMore;
    
    // 스타일 적용
    if (isLoading) {
        // 로딩 중
        loadMoreBtn.style.background = '#6c757d';
        loadMoreBtn.style.color = 'white';
        loadMoreBtn.style.cursor = 'not-allowed';
        loadMoreBtn.textContent = '로딩 중...';
    } else if (hasMore) {
        // 더 로드할 데이터가 있음
        loadMoreBtn.style.background = '#007bff';
        loadMoreBtn.style.color = 'white';
        loadMoreBtn.style.cursor = 'pointer';
        
        const remainingPages = totalPages - currentPage - 1;
        const remainingMembers = remainingPages * 20; // 페이지당 20개
        loadMoreBtn.textContent = remainingPages > 0 ? 
            `더보기 (약 ${remainingMembers}명 더 있음)` : 
            '더보기';
    } else {
        // 마지막 페이지 - 더 이상 데이터 없음
        loadMoreBtn.style.background = '#e9ecef';
        loadMoreBtn.style.color = '#6c757d';
        loadMoreBtn.style.cursor = 'not-allowed';
        loadMoreBtn.textContent = '모든 데이터를 불러왔습니다';
    }
}


/**
 * 더보기 버튼 클릭 핸들러
 */
async function loadMoreMembers() {
    if (currentPage < totalPages - 1 && !isLoading) {
        const searchTerm = searchInput.value.trim();
        
        // 검색어가 있거나 필터가 적용된 경우 검색 API 사용
        if (searchTerm || currentMemberType !== 'all') {
            await loadMoreWithSearch(currentPage + 1, searchTerm);
        } else {
            // 일반 목록의 다음 페이지 로드
            await loadMembers(currentPage + 1, true);
        }
    }
}

/**
 * 검색 상태에서 더보기 (append 모드)
 */
async function loadMoreWithSearch(page, searchTerm) {
    if (isLoading) return;
    
    try {
        isLoading = true;
        showLoading(true);
        
        // API URL 구성
        const params = new URLSearchParams();
        params.append('page', page);
        params.append('size', '20');
        params.append('sort', 'createdAt,desc');
        
        if (searchTerm) {
            params.append('search', searchTerm);
        }
        
        if (currentMemberType !== 'all') {
            params.append('memberType', currentMemberType);
        }
        
        const response = await fetch(`/api/v1/members?${params.toString()}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        // 페이징된 응답 처리
        if (data.content && Array.isArray(data.content)) {
            // 기존 데이터에 새 데이터 추가
            allMembers = [...allMembers, ...data.content];
            currentPage = data.number;
            totalPages = data.totalPages;
            isLastPage = data.last;
            
            console.log(`더보기 로드 완료: 페이지 ${currentPage + 1}/${totalPages}, 총 회원 수: ${allMembers.length}`);
        } else {
            throw new Error('잘못된 응답 형식입니다.');
        }
        
        displayMembers(allMembers);
        updateLoadMoreButton();
        
    } catch (error) {
        console.error('더보기 로드 실패:', error);
        showNotification('추가 데이터를 불러오는데 실패했습니다: ' + error.message, 'error');
    } finally {
        isLoading = false;
        showLoading(false);
        updateLoadMoreButton();
    }
}

/**
 * 검색 조건으로 회원 목록 로드
 */
async function loadMembersWithSearch(page = 0, append = false, searchTerm = null) {
    if (isLoading) return;
    
    try {
        isLoading = true;
        showLoading(true);
        
        // 검색어가 없으면 입력창에서 가져오기
        if (searchTerm === null) {
            searchTerm = searchInput.value.trim();
        }
        
        // API URL 구성
        const params = new URLSearchParams();
        params.append('page', page);
        params.append('size', '20');
        params.append('sort', 'createdAt,desc');
        
        if (searchTerm) {
            params.append('search', searchTerm);
        }
        
        if (currentMemberType !== 'all') {
            params.append('memberType', currentMemberType);
        }
        
        const response = await fetch(`/api/v1/members?${params.toString()}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        // 페이징된 응답 처리
        if (data.content && Array.isArray(data.content)) {
            if (append) {
                allMembers = [...allMembers, ...data.content];
            } else {
                allMembers = data.content;
            }
            
            currentPage = data.number;
            totalPages = data.totalPages;
            isLastPage = data.last;
            
            console.log(`회원 검색 결과: 페이지 ${currentPage + 1}/${totalPages}, 총 ${data.totalElements}명`);
        } else {
            throw new Error('잘못된 응답 형식입니다.');
        }
        
        displayMembers(allMembers);
        updateLoadMoreButton();
        
    } catch (error) {
        console.error('검색 실패:', error);
        showNotification('검색 중 오류가 발생했습니다: ' + error.message, 'error');
        
        // 검색 실패 시 빈 상태 표시
        if (!append) {
            allMembers = [];
            displayEmptyState();
        }
    } finally {
        isLoading = false;
        showLoading(false);
    }
}

/**
 * 회원 상세 정보 API 조회
 */
async function fetchMemberDetails(memberId) {
    try {
        const response = await fetch(`/api/v1/members/${memberId}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        return await response.json();
        
    } catch (error) {
        console.error('회원 상세 정보 로드 실패:', error);
        throw error;
    }
}

/**
 * 회원 목록 필터링 및 표시 (API 호출)
 */
async function filterAndDisplayMembers() {
    // 검색 함수를 재사용하여 필터링된 결과 가져오기
    await handleSearch();
}

/**
 * 회원 목록을 테이블 형태로 화면에 표시
 */
function displayMembers(members) {
    console.log('displayMembers 호출됨, 회원 수:', members.length);
    console.log('membersList 요소:', membersList);
    
    if (!membersList) {
        console.error('membersList 요소가 없습니다!');
        return;
    }
    
    if (members.length === 0) {
        console.log('회원 데이터가 없어서 빈 상태 표시');
        displayEmptyState();
        return;
    }
    
    const membersHtml = members.map(member => `
        <tr onclick="showMemberDetails(${member.id})" style="cursor: pointer;">
            <td>${member.id}</td>
            <td><strong>${escapeHtml(member.name)}</strong></td>
            <td>${escapeHtml(member.email)}</td>
            <td>${escapeHtml(member.phoneNumber)}</td>
            <td>
                <span class="member-type-badge ${member.memberType.toLowerCase()}">
                    ${getMemberTypeText(member.memberType)}
                </span>
            </td>
            <td>${formatDate(member.createdAt)}</td>
            <td>
                <button class="action-btn detail" onclick="event.stopPropagation(); showMemberDetails(${member.id})">
                    상세보기
                </button>
            </td>
        </tr>
    `).join('');
    
    console.log('HTML 생성 완료, 길이:', membersHtml.length);
    membersList.innerHTML = membersHtml;
    console.log('DOM에 HTML 삽입 완료');
}

/**
 * 회원 상세 정보 표시 (API 호출)
 */
async function showMemberDetails(memberId) {
    try {
        // 로딩 표시
        document.getElementById('memberDetails').innerHTML = `
            <div class="loading-detail">
                <div class="spinner"></div>
                <p>회원 정보를 불러오는 중...</p>
            </div>
        `;
        memberModal.style.display = 'block';
        
        // API에서 상세 정보 가져오기
        const member = await fetchMemberDetails(memberId);
        
        const detailsHtml = `
            <div class="member-detail-card">
                <div class="detail-header">
                    <h4>${escapeHtml(member.name)}</h4>
                    <span class="member-type ${member.memberType.toLowerCase()}">${getMemberTypeText(member.memberType)}</span>
                </div>
                <div class="detail-body">
                    <div class="detail-row">
                        <label>회원 ID:</label>
                        <span>${member.id}</span>
                    </div>
                    <div class="detail-row">
                        <label>이메일:</label>
                        <span>${escapeHtml(member.email)}</span>
                    </div>
                    <div class="detail-row">
                        <label>휴대폰:</label>
                        <span>${escapeHtml(member.phoneNumber)}</span>
                    </div>
                    <div class="detail-row">
                        <label>회원 유형:</label>
                        <span>${getMemberTypeText(member.memberType)}</span>
                    </div>
                    <div class="detail-row">
                        <label>가입일:</label>
                        <span>${formatDateTime(member.createdAt)}</span>
                    </div>
                    <div class="detail-row">
                        <label>수정일:</label>
                        <span>${formatDateTime(member.updatedAt)}</span>
                    </div>
                </div>
            </div>
        `;
        
        document.getElementById('memberDetails').innerHTML = detailsHtml;
        
    } catch (error) {
        console.error('회원 상세 정보 표시 실패:', error);
        showNotification('회원 상세 정보를 불러오는데 실패했습니다: ' + error.message, 'error');
        
        // 오류 시 모달 닫기
        memberModal.style.display = 'none';
    }
}

/**
 * 검색 처리 (서버 사이드 검색 + 페이징)
 */
async function handleSearch() {
    const searchTerm = searchInput.value.trim();
    
    if (searchTerm.length > 0 && searchTerm.length < 2) {
        showNotification('검색어는 2글자 이상 입력해주세요.', 'warning');
        return;
    }
    
    // 검색 시 페이지 초기화
    currentPage = 0;
    isLastPage = false;
    
    if (isLoading) return;
    
    try {
        isLoading = true;
        showLoading(true);
        
        // API URL 구성
        const params = new URLSearchParams();
        params.append('page', 0);
        params.append('size', '20');
        params.append('sort', 'createdAt,desc');
        
        if (searchTerm) {
            params.append('search', searchTerm);
        }
        
        if (currentMemberType !== 'all') {
            params.append('memberType', currentMemberType);
        }
        
        const response = await fetch(`/api/v1/members?${params.toString()}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        // 페이징된 응답 처리
        if (data.content && Array.isArray(data.content)) {
            allMembers = data.content;
            currentPage = data.number;
            totalPages = data.totalPages;
            isLastPage = data.last;
            
            console.log(`회원 검색 결과: 페이지 ${currentPage + 1}/${totalPages}, 총 ${data.totalElements}명, isLastPage: ${isLastPage}`);
        } else {
            throw new Error('잘못된 응답 형식입니다.');
        }
        
        displayMembers(allMembers);
        updateLoadMoreButton(); // 여기가 중요!
        
    } catch (error) {
        console.error('검색 실패:', error);
        showNotification('검색 중 오류가 발생했습니다: ' + error.message, 'error');
        
        // 검색 실패 시 빈 상태 표시
        allMembers = [];
        displayEmptyState();
        
        // 에러 시에도 페이징 상태 초기화
        currentPage = 0;
        totalPages = 1;
        isLastPage = true;
    } finally {
        isLoading = false;
        showLoading(false);
        updateLoadMoreButton(); // 에러 시에도 버튼 상태 업데이트
    }
}

/**
 * 회원 유형 텍스트 변환
 */
function getMemberTypeText(memberType) {
    switch (memberType) {
        case 'STUDENT': return '학생';
        case 'INSTRUCTOR': return '강사';
        default: return memberType;
    }
}

/**
 * 날짜 포맷팅
 */
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR');
}

/**
 * 날짜시간 포맷팅
 */
function formatDateTime(dateString) {
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR');
}

/**
 * 빈 상태 표시 (테이블 형태)
 */
function displayEmptyState() {
    membersList.innerHTML = `
        <tr>
            <td colspan="7" style="text-align: center; padding: 3rem;">
                <div class="empty-state">
                    <div class="empty-icon">👥</div>
                    <h3>회원이 없습니다</h3>
                    <p>검색 조건을 변경하거나 새로운 회원을 등록해보세요.</p>
                </div>
            </td>
        </tr>
    `;
}

/**
 * 서버 연결 실패 시 에러 상태 표시
 */
function displayEmptyStateWithError() {
    membersList.innerHTML = `
        <tr>
            <td colspan="7" style="text-align: center; padding: 3rem;">
                <div class="empty-state">
                    <div class="empty-icon" style="font-size: 4rem;">⚠️</div>
                    <h3 style="color: #dc3545;">서버 연결 실패</h3>
                    <p>서버가 실행되지 않았거나 연결에 문제가 있습니다.</p>
                    <p style="font-size: 14px; color: #6c757d; margin-top: 1rem;">
                        서버를 실행한 후 페이지를 새로고침해주세요.
                    </p>
                    <button onclick="location.reload()" style="
                        margin-top: 1rem;
                        padding: 8px 16px;
                        background: #007bff;
                        color: white;
                        border: none;
                        border-radius: 4px;
                        cursor: pointer;
                    ">새로고침</button>
                </div>
            </td>
        </tr>
    `;
}

/**
 * 로딩 상태 표시/숨김
 */
function showLoading(show) {
    loadingMembers.style.display = show ? 'flex' : 'none';
}

/**
 * 알림 메시지 표시
 */
function showNotification(message, type = 'info') {
    // 콘솔에도 로그 출력
    console.log(`[${type.toUpperCase()}] ${message}`);
    
    // 간단한 알림 (실제로는 더 나은 UI 라이브러리 사용 권장)
    if (type === 'error') {
        alert('❌ ' + message);
    } else if (type === 'warning') {
        alert('⚠️ ' + message);
    } else {
        alert('ℹ️ ' + message);
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
 * 회원 목록 필터링 및 표시 (검색 함수 재사용)
 */
async function filterAndDisplayMembers() {
    console.log('필터링 및 표시 시작, 현재 필터:', currentMemberType);
    await handleSearch();
}
