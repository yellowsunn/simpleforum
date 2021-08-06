package com.yellowsunn.simpleforum.api.controller;

import com.yellowsunn.simpleforum.api.argumentresolver.LoginId;
import com.yellowsunn.simpleforum.api.dto.comment.CommentGetDto;
import com.yellowsunn.simpleforum.api.dto.comment.CommentUploadDto;
import com.yellowsunn.simpleforum.api.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<Void> upload(@LoginId Long userId,
                                       @Validated @RequestBody CommentUploadDto commentUploadDto,
                                       BindingResult bindingResult) {
        checkValidation(bindingResult);
        Long id = commentService.upload(userId, commentUploadDto);
        return ResponseEntity.created(URI.create("/api/comments/" + id))
                .body(null);
    }

    @GetMapping
    public Page<CommentGetDto> comments(@RequestParam Long postId, Pageable pageable) {
        return commentService.getCommentsByPostId(postId, pageable);
    }

    private void checkValidation(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new IllegalArgumentException("validation error");
        }
    }
}