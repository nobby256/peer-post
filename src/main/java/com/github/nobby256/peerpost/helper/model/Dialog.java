package com.github.nobby256.peerpost.helper.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.nobby256.peerpost.helper.model.Dialog.Element.ElementType;

import lombok.Data;
import lombok.Getter;

@Data
public class Dialog {

	/**
	 * Title of the dialog.
	 *
	 * Maximum 24 characters.
	 */
	@JsonProperty("title")
	@Getter
	private String title;

	/**
	 * Up to 5 elements allowed per dialog.
	 *
	 * See below for more details on elements.
	 * If none are supplied - the dialog box acts as a simple confirmation
	 */
	@JsonProperty("elements")
	@Getter
	private List<Element> elements = new ArrayList<>();

	/**
	 * (Optional) The URL of the icon used for your dialog.
	 *
	 * If none specified, no icon is displayed.
	 */
	@JsonProperty("icon_url")
	private String iconUrl;

	/** <ID specified by the integration to identify the request>. */
	@JsonProperty("callback_id")
	private String callbackId;

	/**
	 * (Optional) Label of the button to complete the dialog.
	 *
	 * Default is Submit.
	 */
	@JsonProperty("submit_label")
	private String submitLabel = "submit";

	/**
	 * (Optional) When true, sends an event back to the integration whenever there’s a user-induced dialog cancellation.
	 *
	 * No other data is sent back with the event. Default is false.
	 */
	@JsonProperty("notify_on_cancel")
	private boolean notifyOnCancel = false;

	/**
	 * (Optional) String provided by the integration that will be echoed back with dialog submission.
	 *
	 * Default is the empty string.
	 */
	@JsonProperty("state")
	private String state = "";

	public Dialog(String title) {
		this.title = title;
	}

	public Element addElement(ElementType elementType, String displayName, String name) {
		Element element = new Element(elementType, displayName, name);
		elements.add(element);
		return element;
	}

	@Data
	public static class Element {
		/**
		 * Display name of the field shown to the user in the dialog.
		 *
		 * Maximum 24 characters.
		 */
		@JsonProperty("display_name")
		@Getter
		String displayName;

		/**
		 * Name of the field element used by the integration. Maximum 300 characters.
		 *
		 * You should use unique “name” fields in the same dialog.
		 */
		@JsonProperty("name")
		@Getter
		String name;

		/**
		 *
		 */
		@JsonProperty("type")
		ElementType elementType;

		/**
		 * (Optional) One of text, email, number, password (as of v5.14),``tel``, or url. Default is text.
		 *
		 * Use this to set which keypad is presented to users on mobile when entering the field.
		 */
		@JsonProperty("subtype")
		SubType subType = SubType.text;

		/**
		 * 	(Optional) Minimum input length allowed for an element.
		 *
		 * Default is 0.
		 */
		@JsonProperty("min_length")
		Integer minLength = 0;

		/**
		 * 	(Optional) Maximum input length allowed for an element.
		 *
		 * Default is 150. If you expect the input to be greater 150 characters,
		 * consider using a textarea type element instead.
		 */
		@JsonProperty("max_length")
		Integer maxLength = 150;

		/**
		 * (Optional) Set to true if this form element is not required. Default is false.
		 */
		@JsonProperty("optional")
		boolean optional = false;

		/**
		 * (Optional) Set help text for this form element.
		 *
		 * Maximum 150 characters.
		 */
		@JsonProperty("help_text")
		String helpText;

		/**
		 * (Optional) Set a default value for this form element.
		 *
		 * Maximum 150 characters.
		 */
		@JsonProperty("default")
		String defaultValue;

		/**
		 * (Optional) A string displayed to help guide users in completing the element.
		 *
		 * Maximum 150 characters.
		 */
		@JsonProperty("placeholder")
		String placeholder;

		/**
		 * (Optional) One of users, or channels.
		 *
		 * If none specified, assumes a manual list of options is provided by the integration.
		 */
		@JsonProperty("data_source")
		DataSource dataSource;

		@JsonProperty("options")
		@Getter
		List<Option> options = new ArrayList<>();

		public Element(ElementType elementType, String displayName, String name) {
			this.elementType = elementType;
			this.displayName = displayName;
			this.name = name;
		}

		public Option addOption(String text, String value) {
			Option option = new Option(text, value);
			options.add(option);
			return option;
		}

		public static enum ElementType {
			text, textarea, select, bool
		}

		public static enum SubType {
			text, email, number, password, tel, url
		}

		public static enum DataSource {
			users, channels
		}

		@Data
		public static class Option {
			@JsonProperty("text")
			private String text;
			@JsonProperty("value")
			private String value;

			public Option(String text, String value) {
				this.text = text;
				this.value = value;
			}
		}

	}

}
