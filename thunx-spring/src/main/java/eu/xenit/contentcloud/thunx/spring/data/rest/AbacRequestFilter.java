package eu.xenit.contentcloud.thunx.spring.data.rest;

import static java.lang.String.format;

import eu.xenit.contentcloud.thunx.encoding.ThunkExpressionDecoder;
import eu.xenit.contentcloud.thunx.predicates.model.ThunkExpression;
import eu.xenit.contentcloud.thunx.spring.data.context.AbacContext;
import eu.xenit.contentcloud.thunx.spring.data.context.EntityContext;
import eu.xenit.contentcloud.thunx.spring.data.context.EntityManagerContext;
import java.io.IOException;
import java.util.Base64;
import javax.persistence.EntityManager;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.Repositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.util.UrlPathHelper;

public class AbacRequestFilter implements Filter {

    private final ThunkExpressionDecoder thunkDecoder;

    private final Repositories repos;
    private final EntityManager em;
    private final PlatformTransactionManager tm;

    public AbacRequestFilter(ThunkExpressionDecoder thunkDecoder, Repositories repos, EntityManager em, PlatformTransactionManager tm) {
        this.thunkDecoder = thunkDecoder;
        this.repos = repos;
        this.em = em;
        this.tm = tm;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException,
            ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;

        String path = new UrlPathHelper().getLookupPathForRequest(request);
        String[] pathElements = path.split("/");
        RepositoryInformation ri = RepositoryUtils.findRepositoryInformation(repos, pathElements[1]);
        if (ri == null) {
            ri = RepositoryUtils.findRepositoryInformation(repos, pathElements[2]); // Toon: why is this case here ?
        }
        if (ri == null) {
            throw new IllegalStateException(format("Unable to resolve entity class: %s", path));
        }
        Class<?> entityClass = ri.getDomainType();

        EntityInformation ei = JpaEntityInformationSupport.getEntityInformation(entityClass, em);
        if (entityClass != null) {
            EntityContext.setCurrentEntityContext(ei);
        }

        EntityManagerContext.setCurrentEntityContext(em, tm);

        String abacContext = request.getHeader("X-ABAC-Context");
        if (abacContext != null) {
            byte[] abacContextBytes = Base64.getDecoder().decode(abacContext);
            // which (version of?) decoder should we use ? -> get that info from JWT or other header ?
            ThunkExpression<Boolean> abacExpression = this.thunkDecoder.decoder(abacContextBytes);
            AbacContext.setCurrentAbacContext(abacExpression);
        }

        filterChain.doFilter(servletRequest, servletResponse);

        AbacContext.clear();
        EntityContext.clear();
    }
}
