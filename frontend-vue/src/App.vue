<script setup>
import { ref, onMounted } from "vue";
import { api, login, getSession, clearSession, canEdit } from "./api";
const posts = ref([]),
  selected = ref(null),
  title = ref(""),
  content = ref(""),
  editing = ref(null);
const email = ref("alice@class.test"),
  password = ref("Classroom!2026"),
  user = ref(null),
  busy = ref(false),
  message = ref("게시판을 불러오는 중입니다.");
const refresh = async () => {
  posts.value = await api.list();
};
// 요청을 실행하는 공통 함수: 성공하면 안내 문구, 실패하면 code에 맞는 문구를 보여줌
async function run(action, success) {
  busy.value = true;
  try {
    await action();
    message.value = success;
  } catch (e) {
    message.value = errorMessage(e);
    if (e.status === 404) {   // 이미 지워진 글이면  선택을 풀고 목록을 다시 불러옴
      selected.value = null;
      await refresh();
    }
  } finally {
    user.value = getSession().user; // 401로 토큰이 지워졌을 수도 있으닉가 화면의 로그인 상태를 다시 맞춤
    busy.value = false;
  }
}
//응답 코드별 안내 문구
function errorMessage(e) {
  if (e.code === "INVALID_CREDENTIALS") return "이메일 또는 비밀번호가 틀렸습니다.";
  if (e.code === "TOKEN_REQUIRED") return "로그인이 필요합니다. 먼저 로그인하세요.";
  if (e.code === "INVALID_TOKEN") return "로그인 정보가 올바르지 않습니다. 다시 로그인하세요.";
  if (e.code === "TOKEN_EXPIRED") return "로그인 시간이 만료되었습니다. 다시 로그인하세요.";
  if (e.status === 403) return "본인이 작성한 글만 수정·삭제할 수 있습니다.";
  if (e.status === 404) return "글이 존재하지 않습니다. 목록을 새로 불러왔습니다.";
  if (e.status === 400) return "입력을 확인하세요: " + e.message;
  return e.message;
}
onMounted(() => run(refresh, "게시판이 준비되었습니다."));
function reset() {
  title.value = "";
  content.value = "";
  editing.value = null;
}
//로그인 안 했으면 안내만 하고 멈춤
function save() {
  if (!user.value) {
    message.value = "글을 쓰려면 먼저 로그인하세요.";
    return;
  }
  run(async () => {
    const input = { title: title.value, content: content.value };
    selected.value = editing.value
      ? await api.update(editing.value, input)
      : await api.create(input);
    reset();
    await refresh();
  }, "게시글을 저장했습니다.");
}
function edit() {
  editing.value = selected.value.id;
  title.value = selected.value.title;
  content.value = selected.value.content;
}
function remove() {
  if (confirm("게시글을 삭제할까요?"))
    run(async () => {
      await api.remove(selected.value.id);
      selected.value = null;
      reset();
      await refresh();
    }, "게시글을 삭제했습니다.");
}
// 이 탭의 메모리에서만 토큰을 지움 (이미 복사된 토큰은 만료 전까지 서버에서는 유효함1111!!!)
function logout() {
  clearSession();
  user.value = null;
  reset(); // 쓰던 글 입력창도 같이 비움
  message.value = "이 탭의 로그인 상태를 해제했습니다.";
}
function me() {
  run(async () => {
    const u = await api.me();
    alert(`${u.name} (${u.email})`);
  }, "내 정보 조회를 완료했습니다.");
}
</script>
<template>
  <main>
    <header>
      <span class="eyebrow">WEB SERVICE DESIGN · STARTER</span>
      <h1>모여봐요 동물의 숲(게임) 정보 교환 게시판</h1>
      <p class="muted">
        필요한 아이템을 구하고, 아이템을 나눔하는 교환 및 정보 공유 게시판입니다.
      </p>
    </header>
    <p class="banner">
      누구나 볼 수 있지만, 로그인한 사용자만 본인 글에 대해 수정, 삭제 작업이 가능합니다
    </p>
    <div role="status" class="message">{{ message }}</div>
    <div class="grid">
      <aside>
        <section class="card">
          <h2>로그인</h2>
          <p>{{ user ? `${user.name}님 로그인 중` : "로그아웃 상태" }}</p>
          <form
            @submit.prevent="
              run(() => login(email, password), '로그인했습니다.')
            "
          >
            <label>이메일<input type="email" v-model="email" required /></label
            ><label
              >비밀번호<input type="password" v-model="password" required
            /></label>
            <div class="actions">
              <button :disabled="busy">로그인</button
              ><button type="button" class="secondary" @click="logout">
                로그아웃
              </button>
            </div>
          </form>
          <p>
            <small
              >테스트 계정: alice@class.test / bob@class.test<br />공통
              비밀번호: Classroom!2026</small
            >
          </p>
          <button class="secondary" :disabled="busy" @click="me">
            내 정보 조회
          </button>
        </section>
        <section class="card">
          <h2>{{ editing ? "게시글 수정" : "새 글 작성" }}</h2>
          <form @submit.prevent="save">
            <label>제목<input maxlength="100" v-model="title" required /></label
            ><label
              >내용<textarea maxlength="5000" v-model="content" required />
            </label>
            <div class="actions">
              <button :disabled="busy">
                {{ editing ? "수정 저장" : "등록" }}</button
              ><button type="button" class="secondary" @click="reset">
                입력 초기화
              </button>
            </div>
          </form>
        </section>
      </aside>
      <div>
        <section class="card">
          <h2>
            게시글 목록 <small>{{ posts.length }}개</small>
          </h2>
          <p v-if="!posts.length">첫 번째 기록을 남겨 보세요.</p>
          <article class="post" v-for="p in posts" :key="p.id">
            <button
              class="post-title"
              :disabled="busy"
              @click="
                run(
                  async () => (selected = await api.detail(p.id)),
                  '게시글을 불러왔습니다.',
                )
              "
            >
              {{ p.title }}
            </button>
            <p>
              <small
                >{{ p.authorName }} ·
                {{ new Date(p.createdAt).toLocaleString() }}</small
              >
            </p>
          </article>
        </section>
        <section class="card">
          <h2>게시글 상세</h2>
          <template v-if="selected"
            ><h3>{{ selected.title }}</h3>
            <p class="content">{{ selected.content }}</p>
            <p>
              <small>작성자 {{ selected.authorName }}</small>
            </p>
            <div class="actions" v-if="canEdit(selected)">
              <button class="secondary" @click="edit">수정</button
              ><button class="danger" :disabled="busy" @click="remove">
                삭제
              </button>
            </div></template
          >
          <p class="muted" v-else>목록에서 제목을 선택하세요.</p>
        </section>
      </div>
    </div>
  </main>
</template>
