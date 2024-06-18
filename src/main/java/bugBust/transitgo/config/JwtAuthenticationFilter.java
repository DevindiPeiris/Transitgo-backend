package bugBust.transitgo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@RequiredArgsConstructor //create constructor for any final field created
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
           @NotNull HttpServletRequest request,
           @NotNull HttpServletResponse response,
           @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        //stores the token which need to send with the header
         final String authHeader = request.getHeader("Authorization");
         final String jwt;
         final String userEmail;
         //Token always starts with Bearer
         if (authHeader == null || !authHeader.startsWith("Bearer ")){
             filterChain.doFilter(request,response);
             return;
         }
         //extract the token by excluding bearer(first 7 characters)
         jwt = authHeader.substring(7);
         userEmail = jwtService.extractUsername(jwt);
         //check whether email is null and user is already authenticated
         if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null){
             UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
             if (jwtService.isTokenValid(jwt, userDetails)){
                 UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                         userDetails,
                         null,
                         userDetails.getAuthorities()
                 );
                  authToken.setDetails(
                          new WebAuthenticationDetailsSource().buildDetails(request)
                  );
                  SecurityContextHolder.getContext().setAuthentication(authToken);
             }
         }
         filterChain.doFilter(request,response);
    }
}
