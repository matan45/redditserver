package com.matan.redditclone.service;

import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matan.redditclone.dto.CommentsDto;
import com.matan.redditclone.exception.PostNotFoundException;
import com.matan.redditclone.exception.SpringRedditException;
import com.matan.redditclone.mapper.CommentMapper;
import com.matan.redditclone.model.Comment;
import com.matan.redditclone.model.NotificationEmail;
import com.matan.redditclone.model.Post;
import com.matan.redditclone.model.User;
import com.matan.redditclone.repository.CommentRepository;
import com.matan.redditclone.repository.PostRepository;
import com.matan.redditclone.repository.UserRepository;

import freemarker.template.TemplateException;

@Service
@AllArgsConstructor
@Transactional
public class CommentService {
	// TODO: Construct POST URL
	private static final String POST_URL = "http://localhost:4200/view-post/";

	private final CommentMapper commentMapper;
	private final PostRepository postRepository;
	private final CommentRepository commentRepository;
	private final UserRepository userRepository;
	private final AuthService authService;
	private final MailContentBuilder mailContentBuilder;
	private final MailService mailService;

	public void createComment(CommentsDto commentsDto) {
		Post post = postRepository.findById(commentsDto.getPostId())
				.orElseThrow(() -> new PostNotFoundException(commentsDto.getPostId().toString()));
		Comment comment = commentMapper.map(commentsDto, post, authService.getCurrentUser());
		commentRepository.save(comment);

		String message;
		try {
			message = mailContentBuilder.build(
					post.getUser().getUsername() + " posted a comment on your post." + POST_URL + post.getPostId());
			sendCommentNotification(message, post.getUser());
		} catch (IOException | TemplateException e) {
			throw new SpringRedditException(
					"Exception occurred when sending mail comment " + post.getUser().getUsername());
		}
	}

	public List<CommentsDto> getCommentByPost(Long postId) {
		Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId.toString()));
		return commentRepository.findByPost(post).stream().map(commentMapper::mapToDto).collect(Collectors.toList());
	}

	public List<CommentsDto> getCommentsByUser(String userName) {
		User user = userRepository.findByUsername(userName).orElseThrow(() -> new UsernameNotFoundException(userName));
		return commentRepository.findAllByUser(user).stream().map(commentMapper::mapToDto).collect(Collectors.toList());
	}

	private void sendCommentNotification(String message, User user) {
		mailService.sendmailsendgrid(
				new NotificationEmail(user.getUsername() + " Commented on your post", user.getEmail(), message));
	}
}
