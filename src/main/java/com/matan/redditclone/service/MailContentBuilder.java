package com.matan.redditclone.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
class MailContentBuilder {

	private Configuration config;

	String build(String message) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException,
			IOException, TemplateException {
		Map<String, Object> model = new HashMap<>();
		model.put("message", message);
		return FreeMarkerTemplateUtils.processTemplateIntoString(config.getTemplate("email-template.ftl"), model);
	}

}