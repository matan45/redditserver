package com.matan.redditclone.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.matan.redditclone.dto.SubredditDto;
import com.matan.redditclone.service.SubredditService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/subreddit/")
@AllArgsConstructor
public class SubredditController {

	private final SubredditService subredditService;

	@GetMapping("query/all")
	public List<SubredditDto> getAllSubreddits() {
		return subredditService.getAll();
	}

	@GetMapping("{id}")
	public SubredditDto getSubreddit(@PathVariable Long id) {
		return subredditService.getSubreddit(id);
	}

	@PostMapping
	public SubredditDto create(@RequestBody @Valid SubredditDto subredditDto) {
		return subredditService.save(subredditDto);
	}
}