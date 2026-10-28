// 로그인 정보는 브라우저 메모리에만 저장 (localStorage에 안 넣어서 새로고침하면 사라짐)
let session = { accessToken: "", user: null };
export const getSession = () => session;
export function clearSession() {
  session = { accessToken: "", user: null };
}
function authorizationHeaders() {
  // 토큰이 있을 때만 Bearer 헤더 추가
  // 토큰이 없으면 빈 객체를 돌려줘서 "Bearer undefined" 같은 잘못된 헤더가 안 가게 함
  if (!session.accessToken) return {};
  return { Authorization: `Bearer ${session.accessToken}` };
}
export async function request(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...authorizationHeaders(),
      ...options.headers,
    },
  });
  const data = response.status === 204 ? null : await response.json();
  if (!response.ok) {
    // 401이면 로그인 상태를 해제시킴 (토큰 없음, 변조, 만료 모두 401)
    // 403은 로그인은 맞고 권한만 없는 거라 로그아웃시키지 않음
    if (response.status === 401) clearSession();
    const error = new Error(
        data.message || data.code || `HTTP ${response.status}`,
    );
    error.status = response.status;
    error.code = data.code;
    throw error;
  }
  return data;
}
export async function login(email, password) {
  // 로그인 요청 후 토큰과 사용자 정보를 메모리에 저장
  // 로그인이 실패하면 request()에서 에러가 나서 아래 저장하는 줄까지 오지 않음
  const data = await request("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
  session = { accessToken: data.accessToken, user: data.user };
  return data.user;
}
export function canEdit(post) {
  // 본인이 작성한 글에만 수정, 삭제 버튼 표시가 뜸
  // 버튼을 숨기는 건 화면에서만이고, 실제 권한 검사는 서버의 assertOwner()가 함
  if (!session.user || !post) return false;
  return session.user.id === post.authorId;
}
export const api = {
  list: () => request("/posts"),
  detail: (id) => request(`/posts/${id}`),
  create: (input) =>
    request("/posts", { method: "POST", body: JSON.stringify(input) }),
  update: (id, input) =>
    request(`/posts/${id}`, { method: "PUT", body: JSON.stringify(input) }),
  remove: (id) => request(`/posts/${id}`, { method: "DELETE" }),
  me: () => request("/me"),
};
