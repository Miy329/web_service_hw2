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
  // JJWT 의존성은 pom.xml에 포함되어 있습니다. app.jwt.* 설정은 application.properties 참조.

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
    if (secret == null || !secret.matches("[0-9a-fA-F]{64}")) { // 비밀키가 16진수거나 없다면 실행을 안함
      throw new IllegalStateException("JWT_SECRET 형식이 잘못되었습니다.");
    }
  }

  // 16진수 문자열을 32바이트 키로 변환
  private SecretKey key() {
    byte[] keyBytes = HexFormat.of().parseHex(secret);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  // 로그인 성공시 JWT 발급하는 함수
  //JWT를 알려면 서버 설정에 있는 비밀 키와 발급자의 값을 알아야 함
  public String issueToken(Users.PublicUser user) {
    // 토큰 발급 시각과 만료 시각
    Date now = new Date();
    Date expiration = new Date(now.getTime() + ttl * 1000);

    // 실제 JWT 생성 후 반환
    return Jwts.builder()
            .subject(String.valueOf(user.id()))
            .issuedAt(now)
            .expiration(expiration)
            .issuer(issuer)
            .audience().add(audience).and()
            .signWith(key(), Jwts.SIG.HS256)
            .compact();
  }


  //JWT를 검증해 로그인 사용자 확인하는 함수
  public long currentUserId(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      throw new ApiError(401, "TOKEN_REQUIRED", "로그인이 필요합니다.");
    }
    String token = header.substring(7).trim();
    if (token.isEmpty()) {
      throw new ApiError(401, "TOKEN_REQUIRED", "로그인이 필요합니다.");
    }

    Claims claims;
    try {
      Jws<Claims> jws = Jwts.parser()
              .verifyWith(key())
              .requireIssuer(issuer)
              .requireAudience(audience)
              .build()
              .parseSignedClaims(token);
      if (!"HS256".equals(jws.getHeader().getAlgorithm())) {
        throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
      }
      claims = jws.getPayload();
    } catch (ExpiredJwtException e) {
      throw new ApiError(401, "TOKEN_EXPIRED", "로그인이 만료되었습니다. 다시 로그인하세요.");
    } catch (JwtException | IllegalArgumentException e) {
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
    }

    String sub = claims.getSubject();
    if (claims.getExpiration() == null || sub == null || !sub.matches("[1-9][0-9]{0,15}")) {
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");
    }
    long id = Long.parseLong(sub);
    if (users.find(id) == null) {
      throw new ApiError(401, "INVALID_TOKEN", "사용자를 확인할 수 없습니다.");
    }
    return id;
  }

  //본인 게시글인지 확인하는 함수
  public void assertOwner(long userId, long authorId) {
    if (userId != authorId) {
      throw new ApiError(403, "FORBIDDEN", "본인 글만 수정·삭제할 수 있습니다.");
    }
  }
}