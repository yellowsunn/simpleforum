package com.yellowsunn.simpleforum.domain.comment;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.yellowsunn.simpleforum.domain.posts.Posts;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.Optional;

import static com.yellowsunn.simpleforum.domain.comment.QComment.comment;

public class CommentRepositoryCustomImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public CommentRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Optional<Comment> findByIdQuery(Long id) {
        Comment findComment = queryFactory.selectFrom(comment)
                .leftJoin(comment.user).fetchJoin()
                .where(comment.id.eq(id))
                .fetchOne();

        return Optional.ofNullable(findComment);
    }

    @Override
    public void deleteAllByParentIdQuery(Long parentId) {
        queryFactory.delete(comment)
                .where(comment.parent.id.eq(parentId))
                .execute();
    }

    @Modifying
    @Transactional
    @Override
    public void deleteAllByPostQuery(Posts post) {
        queryFactory.delete(comment)
                .where(comment.parent.id.isNotNull(), comment.post.eq(post))
                .execute();

        queryFactory.delete(comment)
                .where(comment.post.eq(post))
                .execute();
    }
}
