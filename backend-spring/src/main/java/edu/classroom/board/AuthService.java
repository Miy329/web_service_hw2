package edu.classroom.board;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

//JWT 발급을 위해 추가한 라이브러리
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HexFormat;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;


// 누구인지 확인
@Service
public class AuthService {
  // 설정값은 application.properties(환경변수)에서 읽음

  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.issuer}")
  private String issuer;

  @Value("${app.jwt.audience}")
  private String audience;

  @Value("${app.jwt.ttl}")
  private long ttl;

  private final Users users;

  public AuthService(Users users) {
    this.users = users;
  }

  @PostConstruct
  void checkSecret() { // 서버가 켜지면 딱 한번 실행되는 것
    if (secret == null || !secret.matches("[0-9a-fA-F]{64}")) { // 비밀키가 64자리 16진수가 아니거나 없으면 실행을 안함
      throw new IllegalStateException("JWT_SECRET 형식이 잘못되었습니다.");
    }
  }

  // 16진수 문자열을 32바이트 키로 변환
  private SecretKey key() {
    byte[] keyBytes = HexFormat.of().parseHex(secret);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  // 로그인 성공시 JWT 발급하는 함수
  public String issueToken(Users.PublicUser user) {
    // 토큰 발급 시각과 만료 시각
    Date now = new Date();
    Date expiration = new Date(now.getTime() + ttl * 1000); // ttl은 초 단위, Date는 밀리초라서 1000을 곱함

    // 실제 JWT 생성 후 반환
    return Jwts.builder()
            .subject(String.valueOf(user.id())) // sub에 사용자 id를 넣음 (비밀번호는 절대 안 넣음)
            .issuedAt(now)
            .expiration(expiration)
            .issuer(issuer) // 누가 발급했는지
            .audience().add(audience).and() // 어디에 쓰는 토큰인지
            .signWith(key(), Jwts.SIG.HS256) // 서버 비밀 키로 서명 → 내용을 바꾸면 서명이 안 맞게 됨
            .compact();
  }


  //JWT를 검증해 로그인 사용자 확인하는 함수
  public long currentUserId(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    // 헤더가 없거나 Bearer 형식이 아니면 누군지 알 수 없으니 401 TOKEN_REQUIRED
    if (header == null || !header.startsWith("Bearer ")) {
      throw new ApiError(401, "TOKEN_REQUIRED", "로그인이 필요합니다.");
    }
    String token = header.substring(7).trim(); // "Bearer " 7글자 뒤가 실제 토큰
    if (token.isEmpty()) {
      throw new ApiError(401, "TOKEN_REQUIRED", "로그인이 필요합니다.");
    }

    Claims claims;
    try {
      // decode처럼 읽기만 하는 게 아니라 verify로 서명, 만료, 발급자, 사용 대상까지 검사
      Jws<Claims> jws = Jwts.parser()
              .verifyWith(key())
              .requireIssuer(issuer)
              .requireAudience(audience)
              .build()
              .parseSignedClaims(token);
      if (!"HS256".equals(jws.getHeader().getAlgorithm())) { // 허용하는 서명 방식은 HS256 하나
        throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
      }
      claims = jws.getPayload();
    } catch (ExpiredJwtException e) { // 서명은 맞지만 시간이 지난 토큰은 따로 구분
      throw new ApiError(401, "TOKEN_EXPIRED", "로그인이 만료되었습니다. 다시 로그인하세요.");
    } catch (JwtException | IllegalArgumentException e) { // 변조, 발급자, 사용 대상 오류 등 나머지
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
    }

    // sub가 양의 정수 id인지, 그 사용자가 실제로 있는지까지 확인
    String sub = claims.getSubject();
    if (claims.getExpiration() == null || sub == null || !sub.matches("[1-9][0-9]{0,15}")) {
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
    }
    long id = Long.parseLong(sub);
    if (users.find(id) == null) {
      throw new ApiError(401, "INVALID_TOKEN", "사용자를 확인할 수 없습니다.");
    }
    return id; // 여기서 나온 id만 작성자 저장과 권한 검사에 사용 (요청 본문의 값은 안 믿음)
  }

  //본인 게시글인지 확인하는 함수
  // 401은 누군지 모를 때, 403은 누군지는 알지만 그 글의 작성자가 아닐 때
  public void assertOwner(long userId, long authorId) {
    if (userId != authorId) { // 토큰 속 사용자와 DB의 작성자가 다르면 DB를 바꾸기 전에 막음
      throw new ApiError(403, "FORBIDDEN", "본인 글만 수정·삭제할 수 있습니다.");
    }
  }
}