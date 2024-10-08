package com.padaks.todaktodak.post.repository;

import com.padaks.todaktodak.post.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByDeletedTimeAtIsNull(Pageable pageable);
}
