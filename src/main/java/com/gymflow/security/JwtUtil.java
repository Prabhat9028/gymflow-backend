package com.gymflow.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.expiration}") private long expiration;

    private SecretKey getSigningKey() {
        byte[] k = secret.getBytes(StandardCharsets.UTF_8);
        if (k.length < 32) { byte[] p = new byte[32]; System.arraycopy(k, 0, p, 0, k.length); k = p; }
        return Keys.hmacShaKeyFor(k);
    }
    public String generateToken(UUID userId, String email, String role, UUID companyId, UUID branchId) {
        var b = Jwts.builder().subject(email).claim("userId", userId.toString()).claim("role", role);
        if (companyId != null) b.claim("companyId", companyId.toString());
        if (branchId != null) b.claim("branchId", branchId.toString());
        return b.issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expiration)).signWith(getSigningKey()).compact();
    }
    public Claims extractClaims(String t) { return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(t).getPayload(); }
    public String getEmail(String t) { return extractClaims(t).getSubject(); }
    public String getRole(String t) { return extractClaims(t).get("role", String.class); }
    public boolean isValid(String t) { try { extractClaims(t); return true; } catch (JwtException e) { return false; } }
}
