/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.download;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.AjaxDownloadBehavior.Location;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.validation.validator.StringValidator;

import com.antilia.widgets.button.AjaxDownloadButton;
import com.antilia.widgets.button.ButtonBehavior;
import com.antilia.widgets.button.ButtonBehavior.Size;
import com.antilia.widgets.button.ButtonBehavior.Variant;
import com.antilia.widgets.button.ButtonDownloadBehavior;
import com.antilia.widgets.demo.DemoCases;
import com.antilia.widgets.demo.DemoPage;
import com.antilia.widgets.dropdown.Dropdown;
import com.antilia.widgets.icon.IIcon;
import com.antilia.widgets.icon.SvgIcon;
import com.antilia.widgets.notification.Notification;

/**
 * Downloads of every kind, all from the buttons of the theme, in the look of the session; the
 * files are a report of the countries of the world, in seven formats (see {@link DemoFile}), made
 * on the server when they are asked for. Each kind is a case of {@link DemoCases}:
 * <ul>
 * <li>via Ajax: an {@link AjaxDownloadButton} in each {@link Location} of Wicket's
 * {@code AjaxDownloadBehavior}, into a blob, through a hidden iframe, in a new tab and in the same
 * window, busy while the download runs, for a file chosen in a drop-down; the server answers at
 * once, slowly or fails, as another drop-down says, and a notification tells how the download
 * ended;</li>
 * <li>links, drawn as buttons by {@link ButtonBehavior}: a {@link DownloadLink} of a file
 * written on the server and deleted once downloaded, a {@link ResourceLink} to a resource of the
 * page, a link to the {@link DownloadsResource} at a URL to share, a link opening a file in a new
 * tab and a link with the {@code download} attribute to an image of the web application;</li>
 * <li>after a form: the buttons of a form, {@link AjaxButton}s with a
 * {@link ButtonDownloadBehavior}, download the report the form describes, or open it in a new
 * tab, once the form is valid; its errors are notifications.</li>
 * </ul>
 */
public class DownloadsPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The image of the web application the static link downloads: the screenshot of this demo. */
	static final String IMAGE = "images/demos/downloads.png";

	/** The longest title of a report. */
	static final int TITLE_LENGTH = 60;

	/** The buttons of the Ajax case: where each downloads, its icon and its variant. */
	private static final List<LocationSample> LOCATIONS = List.of(
		new LocationSample(Location.Blob, SvgIcon.DOWNLOAD, Variant.PRIMARY),
		new LocationSample(Location.IFrame, SvgIcon.FILE_ARROW_DOWN, Variant.SECONDARY),
		new LocationSample(Location.NewWindow, SvgIcon.ARROW_UP_RIGHT_FROM_SQUARE,
			Variant.SECONDARY),
		new LocationSample(Location.SameWindow, SvgIcon.WINDOW_MAXIMIZE, Variant.SECONDARY));

	/** The file the Ajax case downloads. */
	private DemoFile file = DemoFile.CSV;

	/** How the server answers in the Ajax case. */
	private Answer answer = Answer.AT_ONCE;

	/** The title of the report of the form. */
	private String title;

	/** The format of the report of the form. */
	private DemoFile format = DemoFile.PDF;

	/** How many countries the report of the form lists. */
	private Integer rows = 25;

	/**
	 * Constructor.
	 */
	public DownloadsPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		DemoCases cases = new DemoCases("cases", this);
		themed.add(cases);
		addAjaxCase(cases.addCase("ajax", "ajaxCase"));
		addLinksCase(cases.addCase("links", "linksCase"));
		addFormCase(cases.addCase("form", "formCase"));
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		if (title == null)
		{
			title = getString("file.title");
		}
	}

	/**
	 * The Ajax case: a button for each location, the file and the answer of the server chosen
	 * above them.
	 */
	private void addAjaxCase(Fragment ajaxCase)
	{
		ajaxCase.add(choice("file", LambdaModel.of(() -> file, chosen -> file = chosen),
			List.of(DemoFile.values()), null));

		WebMarkupContainer locations = new WebMarkupContainer("locations");
		locations.setOutputMarkupId(true);
		ajaxCase.add(locations);
		// the same window cannot tell a failure: the button waits while the server fails
		ajaxCase.add(choice("answer", LambdaModel.of(() -> answer, chosen -> answer = chosen),
			List.of(Answer.values()), target -> target.add(locations)));

		locations.add(new ListView<>("locations", LOCATIONS)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<LocationSample> item)
			{
				LocationSample sample = item.getModelObject();
				String key = sample.key();
				item.add(new AjaxDownloadButton("download", new ResourceModel(key),
					new AjaxFileResource(sample.location()))
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void onConfigure()
					{
						super.onConfigure();
						setEnabled(!sample.blockedBy(answer));
					}

					@Override
					protected void onDownloadSuccess(AjaxRequestTarget target)
					{
						notifyDownload(true, file.fileName("countries"), getString(key));
					}

					@Override
					protected void onDownloadFailed(AjaxRequestTarget target)
					{
						notifyDownload(false, file.fileName("countries"), getString(key));
					}
				}.setLocation(sample.location()).setIcon(sample.icon()).setVariant(sample.variant()));
				item.add(new Label("help", new ResourceModel(key + ".help")));
				item.add(new Label("blocked", new ResourceModel(key + ".blocked", ""))
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void onConfigure()
					{
						super.onConfigure();
						setVisible(sample.blockedBy(answer));
					}
				});
			}
		}.setReuseItems(true));
	}

	/**
	 * The links case: links drawn as buttons, which the browser follows without Ajax.
	 */
	private void addLinksCase(Fragment linksCase)
	{
		DownloadLink fileLink = new DownloadLink("fileLink", new LoadableDetachableModel<File>()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected File load()
			{
				return writeTemporaryFile(DemoFile.TEXT);
			}
		}, DemoFile.TEXT.fileName("countries"));
		fileLink.setDeleteAfterDownload(true);
		linksCase.add(asButton(fileLink, DemoFile.TEXT.getIcon(), "fileLink", Variant.PRIMARY));

		linksCase.add(asButton(new ResourceLink<Void>("resourceLink",
			new PageFileResource(DemoFile.CSV)), DemoFile.CSV.getIcon(), "resourceLink",
			Variant.SECONDARY));

		PageParameters json = DownloadsResource.parameters(DemoFile.JSON, false);
		linksCase.add(asButton(new ResourceLink<Void>("mountedLink", DownloadsResource.REFERENCE,
			json), DemoFile.JSON.getIcon(), "mountedLink", Variant.SECONDARY));
		linksCase.add(new Label("mountedUrl", () -> getRequestCycle().getUrlRenderer()
			.renderFullUrl(getRequestCycle().mapUrlFor(DownloadsResource.REFERENCE, json))));

		linksCase.add(asButton(new ResourceLink<Void>("openLink", DownloadsResource.REFERENCE,
			DownloadsResource.parameters(DemoFile.PDF, true)), SvgIcon.ARROW_UP_RIGHT_FROM_SQUARE,
			"openLink", Variant.SECONDARY));

		linksCase.add(asButton(new ExternalLink("staticLink", () -> getRequestCycle()
			.getUrlRenderer().renderContextRelativeUrl(IMAGE)), SvgIcon.FILE_IMAGE, "staticLink",
			Variant.SECONDARY).add(AttributeModifier.replace("download", "downloads.png")));
	}

	/**
	 * The form case: a report described by a form, downloaded or opened by its buttons once the
	 * form is valid.
	 */
	private void addFormCase(Fragment formCase)
	{
		Form<Void> form = new Form<>("report");
		formCase.add(form);
		form.add(new RequiredTextField<>("title", LambdaModel.of(() -> title,
			value -> title = value)).add(StringValidator.maximumLength(TITLE_LENGTH))
			.setLabel(new ResourceModel("report.title")));
		form.add(choice("format", LambdaModel.of(() -> format, chosen -> format = chosen),
			List.of(DemoFile.values()), null).setLabel(new ResourceModel("report.format")));
		form.add(new NumberTextField<>("rows", LambdaModel.of(() -> rows, value -> rows = value),
			Integer.class).setMinimum(1).setMaximum(CountriesReport.ALL).setRequired(true)
			.setLabel(new ResourceModel("report.rows")));
		form.add(submitButton("download", new ReportResource(ContentDisposition.ATTACHMENT),
			Location.Blob, SvgIcon.DOWNLOAD, Variant.PRIMARY));
		form.add(submitButton("open", new ReportResource(ContentDisposition.INLINE),
			Location.NewWindow, SvgIcon.ARROW_UP_RIGHT_FROM_SQUARE, Variant.SECONDARY));
	}

	/**
	 * @return a button of the form: it sends the form, and downloads the report once the form is
	 *         valid, busy meanwhile
	 */
	private AjaxButton submitButton(String id, ReportResource resource, Location location,
		IIcon icon, Variant variant)
	{
		String key = "report." + id;
		String where = "location." + location.name().toLowerCase(Locale.ROOT);
		ButtonDownloadBehavior download = new ButtonDownloadBehavior(resource)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onDownloadSuccess(AjaxRequestTarget target)
			{
				notifyDownload(true, format.fileName(reportName()), getString(where));
			}

			@Override
			protected void onDownloadFailed(AjaxRequestTarget target)
			{
				notifyDownload(false, format.fileName(reportName()), getString(where));
			}
		};
		download.setLocation(location);
		AjaxButton button = new AjaxButton(id)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				download.initiate(target);
			}

			@Override
			public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
			{
				replaceComponentTagBody(markupStream, openTag,
					ButtonBehavior.body(icon, getString(key), false));
			}
		};
		button.add(new ButtonBehavior(variant, Size.MEDIUM), download);
		return button;
	}

	/**
	 * @return a drop-down of ui-components choosing among the constants of an enum, named in the
	 *         bundles; it tells the server each choice, and then updates the page if told to
	 */
	private <T extends Enum<T>> Dropdown<T> choice(String id, IModel<T> model, List<T> choices,
		ChoiceUpdate update)
	{
		Dropdown<T> choice = new Dropdown<>(id, model, choices, new EnumChoiceRenderer<>(this));
		choice.setRequired(true);
		choice.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				if (update != null)
				{
					update.onUpdate(target);
				}
			}
		});
		return choice;
	}

	/**
	 * Shows a link as a button of the theme: its icon and its text, from the bundle under the key.
	 */
	private <T extends AbstractLink> T asButton(T link, IIcon icon, String key, Variant variant)
	{
		link.setBody(() -> ButtonBehavior.body(icon, getString(key), false).toString());
		// the body escapes the text itself
		link.setEscapeModelStrings(false);
		link.add(new ButtonBehavior(variant, Size.MEDIUM));
		return link;
	}

	/**
	 * Tells in a notification how a download ended.
	 */
	private void notifyDownload(boolean success, String fileName, String where)
	{
		IModel<Map<String, String>> values = Model.ofMap(Map.of("file", fileName, "where", where));
		String body = new StringResourceModel(success ? "downloaded" : "failed", this, values)
			.getString();
		getNotifications().show(success
			? Notification.success(body).setTitle(getString("downloaded.title"))
			: Notification.error(body).setTitle(getString("failed.title")));
	}

	/**
	 * @return the name of the file of the report, without its extension: its title in lowercase,
	 *         without accents, its words joined by dashes; {@code report} for a title without
	 *         letters or digits
	 */
	String reportName()
	{
		String name = Normalizer.normalize(title != null ? title : "", Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9]+", "-")
			.replaceAll("(^-+)|(-+$)", "");
		return name.isEmpty() ? "report" : name;
	}

	/**
	 * @return a file of the temporary folder of the server holding the report of all countries in
	 *         a format
	 */
	private File writeTemporaryFile(DemoFile kind)
	{
		try
		{
			File temporary = File.createTempFile("countries-", "." + kind.getExtension());
			Files.write(temporary.toPath(), kind.write(CountriesReport.of(getLocale()),
				"countries"));
			return temporary;
		}
		catch (IOException e)
		{
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(new CssResourceReference(DownloadsPage.class,
			"DownloadsPage.css")));
	}

	/**
	 * What a drop-down does once the server has its choice.
	 */
	@FunctionalInterface
	private interface ChoiceUpdate extends Serializable
	{
		void onUpdate(AjaxRequestTarget target);
	}

	/**
	 * A button of the Ajax case: where it downloads, its icon and its variant.
	 */
	private record LocationSample(Location location, SvgIcon icon, Variant variant)
		implements Serializable
	{
		/**
		 * @return the key of the name of the location, and the prefix of the keys of its texts
		 */
		String key()
		{
			return "location." + location.name().toLowerCase(Locale.ROOT);
		}

		/**
		 * @return whether the button waits while the server answers so: a download in the same
		 *         window cannot tell a failure, the error would take the place of the page
		 */
		boolean blockedBy(Answer answer)
		{
			return location == Location.SameWindow && answer == Answer.FAILS;
		}
	}

	/**
	 * The file of a button of the Ajax case: the file chosen, inline for a new tab and as an
	 * attachment otherwise, the server answering as chosen.
	 */
	private class AjaxFileResource extends DemoFileResource
	{
		private static final long serialVersionUID = 1L;

		private final Location location;

		AjaxFileResource(Location location)
		{
			this.location = location;
		}

		@Override
		protected DemoFile getFile(Attributes attributes)
		{
			return file;
		}

		@Override
		protected ContentDisposition getContentDisposition(Attributes attributes)
		{
			return location == Location.NewWindow ? ContentDisposition.INLINE
				: ContentDisposition.ATTACHMENT;
		}

		@Override
		protected Answer getAnswer()
		{
			return answer;
		}
	}

	/**
	 * A file of the page, of a kind, as an attachment.
	 */
	private static class PageFileResource extends DemoFileResource
	{
		private static final long serialVersionUID = 1L;

		private final DemoFile kind;

		PageFileResource(DemoFile kind)
		{
			this.kind = kind;
		}

		@Override
		protected DemoFile getFile(Attributes attributes)
		{
			return kind;
		}
	}

	/**
	 * The report the form describes, in the format it says, named after its title.
	 */
	private class ReportResource extends DemoFileResource
	{
		private static final long serialVersionUID = 1L;

		private final ContentDisposition disposition;

		ReportResource(ContentDisposition disposition)
		{
			this.disposition = disposition;
		}

		@Override
		protected DemoFile getFile(Attributes attributes)
		{
			return format;
		}

		@Override
		protected CountriesReport getReport(Locale locale)
		{
			return new CountriesReport(title, rows, locale);
		}

		@Override
		protected String getName()
		{
			return reportName();
		}

		@Override
		protected ContentDisposition getContentDisposition(Attributes attributes)
		{
			return disposition;
		}
	}
}
