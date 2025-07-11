package org.myjtools.jexten.processor;

import java.util.*;
import java.util.stream.Stream;
import javax.lang.model.element.Element;

public class Errors {


	private final Map<Element,List<String>> messages = new HashMap<>();
	private final List<String> fixes = new ArrayList<>();


	public void addMessage(Element element, String message, Object... args) {
		this.messages.computeIfAbsent(element, x->new ArrayList<>())
			.add(message.replace("{}","%s").formatted(args));
	}

	public void addFix(String fix, Object... args) {
		this.fixes.add(fix.replace("{}","%s").formatted(args));
	}

	public boolean hasMessages() {
		return !this.messages.isEmpty();
	}

	public boolean hasFixes() {
		return !this.fixes.isEmpty();
	}

	public Map<Element,List<String>> messages() {
		return this.messages;
	}

	public Stream<String> fixes() {
		return this.fixes.stream();
	}




}
