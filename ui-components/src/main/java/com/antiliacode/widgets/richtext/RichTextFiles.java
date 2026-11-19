/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antiliacode.widgets.richtext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.security.SecureRandom;

import org.apache.wicket.Application;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.FetchMetadataResourceIsolationPolicy;
import org.apache.wicket.protocol.http.IResourceIsolationPolicy;
import org.apache.wicket.protocol.http.IResourceIsolationPolicy.ResourceIsolationOutcome;
import org.apache.wicket.protocol.http.OriginResourceIsolationPolicy;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.encoding.UrlEncoder;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiliacode.widgets.upload.IUploadFileFormat;
import com.antiliacode.widgets.upload.IUploadScanner;
import com.antiliacode.widgets.upload.UploadFileFormat;
import com.github.openjson.JSONObject;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The resource of the uploads of the rich text editors of an application, mounted once at a path
 * of its own, not tied to a page: it takes the images users upload, checks them as
 * {@link RichTextImageUploadBehavior} does, has {@link IRichTextImageStore a store} keep them, and
 * serves them by name, to the editors and to any page showing a text, a {@link RichTextView} for
 * example.
 *
 * <pre>
 * // in WebApplication#init()
 * RichTextFiles.mount(this, "rich-text-files", store).setImageMaxSize(Bytes.megabytes(2));
 *
 * // an editor posting its images there rather than to a behavior of its own
 * editor.setUploadResource(RichTextFiles.get());
 *
 * // the store answers the addresses of the resource, and opens an image by its name
 * public String store(FileUpload image, IRichTextImageType type) throws IOException
 * {
 * 	String name = keep(image, type); // a name of the store's own, never the browser's
 * 	return RichTextFiles.get().addressOf(name);
 * }
 *
 * public RichTextServedFile open(String name) throws IOException
 * {
 * 	return mayBeSeen(name) ? RichTextServedFile.of(fileOf(name), typeOf(name), uploadedName(name))
 * 		: null;
 * }
 * </pre>
 *
 * <h2>Uploads</h2> A POST to {@link #getUploadUrl() the path} takes a file as the behavior does:
 * in a form under {@value RichTextImageUploadBehavior#FILE_PARAMETER}, answering JSON,
 * {@code {"url": "..."}} or {@code {"error": "..."}} in the language of the session; and the
 * addresses of the images to delete, to keep permanently, or what to find among those kept, as
 * the editor in the browser posts them ({@code deleteImagesAt}, {@code storePermanentlyAt},
 * {@code findImagesAt}). An editor {@link RichTextEditor#setUploadResource(RichTextFiles) posting its
 * images here} registers its own rules in the session as it renders, its limits, its types and
 * whether it keeps images permanently, and posts with the key of its uploads; it deletes only the
 * images it took, in this session. The editors of React and Svelte post without a key, under the
 * rules of the resource ({@link #setImageMaxSize(Bytes)}, {@link #setImageTypes(Collection)},
 * {@link #setImageMaxDimensions(RichTextImageDimensions)}, {@link #setPermanentImages(boolean)}),
 * each session deleting only the images it took.
 * <p>
 * The resource takes uploads only from the site's own pages: a request a browser sends from
 * another site, as its {@code Sec-Fetch-Site} or its {@code Origin} tells, is refused, since the
 * cross-site checks of a page ({@code ResourceIsolationRequestCycleListener}) leave resources
 * alone. The whole request may be no larger than the largest image allowed and its boundaries:
 * it is cut before the file is read. Who may upload is the application's business besides:
 * {@link #setUploadCheck(Predicate)}, and its authorization strategy as for any resource.
 *
 * <h2>Serving</h2> A GET of the path and a name serves the file of that name the store
 * {@link IRichTextImageStore#open(String) opens}: as the media type of the format its bytes were
 * found to be when it was uploaded, never the one the browser gave; with
 * {@code X-Content-Type-Options: nosniff} and {@code Content-Security-Policy: sandbox}, so that
 * even a file opened in a tab of its own runs no script and reaches neither the cookies nor the
 * pages of the site; {@code inline} for the formats a browser may open in its window
 * ({@link IUploadFileFormat#isInline()}), the images, else as an {@code attachment}, its file name
 * encoded as RFC 6266 says; cached privately, for an hour. Who sees a file is the store's business:
 * it opens only those the user of the request may see; {@link #setReadCheck(BiPredicate)} adds
 * the application's own check. A name the store does not open, or the check refuses, is not found.
 * <p>
 * The links of the files are the site's own by default, {@code /rich-text-files/<name>}; with
 * {@link #setBaseUrl(String) a base address} they start with that, so that an application serves
 * them from a domain of their own ({@code https://files.example.com/rich-text-files}), the strongest
 * isolation. The editors keeping the images of the site alone
 * ({@link AbstractRichTextEditor#setImagesFromSiteOnly(boolean)}) take the addresses below a base
 * address of the application's resources as the site's own ({@link #isOwnUrl(String)}).
 *
 * @since 10.11.0
 */
public final class RichTextFiles
{
	private static final Logger LOG = LoggerFactory.getLogger(RichTextFiles.class);

	/** The name of the parameter of the uploads of an editor, the key it registered its rules by. */
	public static final String KEY_PARAMETER = "antilia-uploads";

	/**
	 * The name of the parameter telling the uploads of files attached,
	 * {@value #ATTACHMENTS}, from those of images.
	 */
	public static final String KIND_PARAMETER = "antilia-kind";

	/** The value of {@link #KIND_PARAMETER} for the files attached. */
	public static final String ATTACHMENTS = "attachments";

	/** The name of the file, the last segment of the path of a GET. */
	private static final String NAME = "name";

	/** The longest name of a file served. */
	private static final int LONGEST_NAME = 255;

	/** The most editors whose rules a session keeps, the oldest forgotten first. */
	private static final int EDITORS = 100;

	/** How long a browser keeps a file served. */
	private static final Duration CACHE = Duration.ofHours(1);

	/** The resources mounted, by application and path. */
	private static final MetaDataKey<Map<String, RichTextFiles>> MOUNTED = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	/** The uploads of a session, by path and key. */
	private static final MetaDataKey<Uploads> UPLOADS = new MetaDataKey<>()
	{
		private static final long serialVersionUID = 1L;
	};

	/** The random keys of the editors. */
	private static final SecureRandom RANDOM = new SecureRandom();

	/** The key of the uploads of the editors of React and Svelte, posting without one. */
	private static final String DEFAULT_KEY = "";

	/** What the key of an editor's files attached ends with, beside that of its images. */
	private static final String ATTACHMENT_SLOT = "\nattachments";

	private final String path;

	private final IRichTextImageStore imageStore;

	private Bytes imageMaxSize = RichTextImageUploadBehavior.DEFAULT_MAX_SIZE;

	private Set<IRichTextImageType> imageTypes = new LinkedHashSet<>(RichTextImageType.ALL);

	private RichTextImageDimensions imageMaxDimensions;

	private boolean permanentImages;

	private int maxFound = RichTextImageUploadBehavior.DEFAULT_MAX_FOUND;

	private String baseUrl;

	private IRichTextAttachmentStore attachmentStore;

	private Bytes attachmentMaxSize = RichTextAttachmentUploadBehavior.DEFAULT_MAX_SIZE;

	private Set<IUploadFileFormat> attachmentFormats = new LinkedHashSet<>(UploadFileFormat.ALL);

	private IUploadScanner scanner;

	private BiPredicate<String, Request> readCheck;

	private Predicate<Request> uploadCheck;

	private RichTextFiles(String path, IRichTextImageStore imageStore)
	{
		this.path = path;
		this.imageStore = imageStore;
	}

	/**
	 * Mounts the resource of the uploads of the rich text editors: once, in
	 * {@link WebApplication#init()}.
	 *
	 * @param application
	 *            the application
	 * @param path
	 *            the path of the resource, below the application: {@code rich-text-files} for
	 *            example; the names of the files follow it
	 * @param imageStore
	 *            what keeps the images and opens them by their names
	 * @return the resource, to set its rules
	 * @throws IllegalStateException
	 *             when a resource of the uploads is mounted at the path already
	 */
	public static RichTextFiles mount(WebApplication application, String path,
		IRichTextImageStore imageStore)
	{
		Args.notNull(application, "application");
		Args.notNull(imageStore, "imageStore");
		String clean = Strings.isEmpty(path) ? "" : path.replaceAll("^/+|/+$", "");
		Args.notEmpty(clean, "path");
		RichTextFiles files = new RichTextFiles(clean, imageStore);
		Map<String, RichTextFiles> mounted = mounted(application);
		if (mounted.putIfAbsent(clean, files) != null)
		{
			throw new IllegalStateException("The uploads of the rich text editors are mounted at " +
				clean + " already");
		}
		ResourceReference reference = new ResourceReference(RichTextFiles.class, clean)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public IResource getResource()
			{
				return new FilesResource(clean);
			}
		};
		// the uploads at the path, the files below it, each by its name
		application.mountResource(clean, reference);
		application.mountResource(clean + "/${" + NAME + "}", reference);
		return files;
	}

	/**
	 * @return the resources mounted in an application, by their paths
	 */
	private static Map<String, RichTextFiles> mounted(Application application)
	{
		synchronized (application)
		{
			Map<String, RichTextFiles> mounted = application.getMetaData(MOUNTED);
			if (mounted == null)
			{
				mounted = new ConcurrentHashMap<>();
				application.setMetaData(MOUNTED, mounted);
			}
			return mounted;
		}
	}

	/**
	 * @return the resource of the uploads the application of this thread mounted, the only one
	 * @throws IllegalStateException
	 *             when it mounted none, or several: then {@link #get(String)} names it
	 */
	/**
	 * Tells whether an address is the site's own for the editors keeping the images of the site
	 * alone ({@link AbstractRichTextEditor#setImagesFromSiteOnly(boolean)}):
	 * {@link RichTextPolicy#isSiteUrl(String) a relative one}, or one below
	 * {@link #setBaseUrl(String) the base address} of a resource the application of the request
	 * mounted, its files on a domain of their own, a name following the base and its slash.
	 *
	 * @param url
	 *            an address
	 * @return whether the address is the site's own
	 * @since 11.0.0
	 */
	public static boolean isOwnUrl(String url)
	{
		if (RichTextPolicy.isSiteUrl(url))
		{
			return true;
		}
		if (url == null || !RichTextPolicy.isSafeUrl(url) || !Application.exists())
		{
			return false;
		}
		Map<String, RichTextFiles> mounted = Application.get().getMetaData(MOUNTED);
		if (mounted == null)
		{
			return false;
		}
		for (RichTextFiles files : mounted.values())
		{
			String base = files.baseUrl;
			if (base != null && url.length() > base.length() + 1 && url.startsWith(base + "/"))
			{
				return true;
			}
		}
		return false;
	}

	public static RichTextFiles get()
	{
		Collection<RichTextFiles> all = mounted(Application.get()).values();
		if (all.size() != 1)
		{
			throw new IllegalStateException(all.isEmpty() ?
				"The application mounted no uploads of the rich text editors: see RichTextFiles.mount" :
				"The application mounted several uploads of the rich text editors: name the path");
		}
		return all.iterator().next();
	}

	/**
	 * @param path
	 *            the path the resource was mounted at
	 * @return the resource of the uploads the application of this thread mounted at the path
	 * @throws IllegalStateException
	 *             when it mounted none there
	 */
	public static RichTextFiles get(String path)
	{
		String clean = path == null ? "" : path.replaceAll("^/+|/+$", "");
		RichTextFiles files = mounted(Application.get()).get(clean);
		if (files == null)
		{
			throw new IllegalStateException("The application mounted no uploads of the rich text " +
				"editors at " + clean);
		}
		return files;
	}

	/**
	 * @return the path the resource is mounted at, below the application
	 */
	public String getPath()
	{
		return path;
	}

	/**
	 * @return what keeps the images
	 */
	public IRichTextImageStore getImageStore()
	{
		return imageStore;
	}

	/**
	 * @return the largest image the editors of React and Svelte upload
	 */
	public Bytes getImageMaxSize()
	{
		return imageMaxSize;
	}

	/**
	 * @param imageMaxSize
	 *            the largest image the editors posting without a key upload, those of React and
	 *            Svelte; {@link RichTextImageUploadBehavior#DEFAULT_MAX_SIZE} by default. Also the
	 *            largest any editor of Wicket uploads here, whatever its own limit.
	 * @return {@code this}
	 */
	public RichTextFiles setImageMaxSize(Bytes imageMaxSize)
	{
		this.imageMaxSize = Args.notNull(imageMaxSize, "imageMaxSize");
		return this;
	}

	/**
	 * @return the types of the images the editors of React and Svelte upload, in their order
	 */
	public Set<IRichTextImageType> getImageTypes()
	{
		return Collections.unmodifiableSet(imageTypes);
	}

	/**
	 * @param imageTypes
	 *            the types of the images the editors posting without a key upload, as their bytes
	 *            say: all those of {@link RichTextImageType} by default
	 * @return {@code this}
	 */
	public RichTextFiles setImageTypes(Collection<? extends IRichTextImageType> imageTypes)
	{
		this.imageTypes = RichTextImageUploads.checkedTypes(imageTypes);
		return this;
	}

	/**
	 * @return the widest and the highest image the editors of React and Svelte upload,
	 *         {@code null} for no limit
	 */
	public RichTextImageDimensions getImageMaxDimensions()
	{
		return imageMaxDimensions;
	}

	/**
	 * @param imageMaxDimensions
	 *            the widest and the highest image the editors posting without a key upload, in
	 *            pixels, as the header of the file says; {@code null} for no limit, as by default
	 * @return {@code this}
	 */
	public RichTextFiles setImageMaxDimensions(RichTextImageDimensions imageMaxDimensions)
	{
		this.imageMaxDimensions = imageMaxDimensions;
		return this;
	}

	/**
	 * @return whether the editors of React and Svelte keep images permanently
	 */
	public boolean isPermanentImages()
	{
		return permanentImages;
	}

	/**
	 * @param permanentImages
	 *            whether the editors posting without a key keep images permanently, and find those
	 *            kept, by the store; {@code false} by default
	 * @return {@code this}
	 */
	public RichTextFiles setPermanentImages(boolean permanentImages)
	{
		this.permanentImages = permanentImages;
		return this;
	}

	/**
	 * @return how many images kept permanently a search answers at most
	 */
	public int getMaxFound()
	{
		return maxFound;
	}

	/**
	 * @param maxFound
	 *            how many images kept permanently a search answers at most, at least one;
	 *            {@link RichTextImageUploadBehavior#DEFAULT_MAX_FOUND} by default
	 * @return {@code this}
	 */
	public RichTextFiles setMaxFound(int maxFound)
	{
		this.maxFound = Math.max(maxFound, 1);
		return this;
	}

	/**
	 * @return what keeps the files attached to the texts, {@code null} for none: the resource takes
	 *         no files
	 */
	public IRichTextAttachmentStore getAttachmentStore()
	{
		return attachmentStore;
	}

	/**
	 * @param attachmentStore
	 *            what keeps the files users attach to the texts, PDFs or spreadsheets, and opens
	 *            them by their names; {@code null} for none, as by default: the resource takes
	 *            images alone
	 * @return {@code this}
	 */
	public RichTextFiles setAttachmentStore(IRichTextAttachmentStore attachmentStore)
	{
		this.attachmentStore = attachmentStore;
		return this;
	}

	/**
	 * @return the largest file the editors of React and Svelte attach
	 */
	public Bytes getAttachmentMaxSize()
	{
		return attachmentMaxSize;
	}

	/**
	 * @param attachmentMaxSize
	 *            the largest file the editors posting without a key attach, those of React and
	 *            Svelte; {@link RichTextAttachmentUploadBehavior#DEFAULT_MAX_SIZE} by default. Also
	 *            the largest any editor of Wicket attaches here, whatever its own limit.
	 * @return {@code this}
	 */
	public RichTextFiles setAttachmentMaxSize(Bytes attachmentMaxSize)
	{
		this.attachmentMaxSize = Args.notNull(attachmentMaxSize, "attachmentMaxSize");
		return this;
	}

	/**
	 * @return the formats of the files the editors of React and Svelte attach, in their order
	 */
	public Set<IUploadFileFormat> getAttachmentFormats()
	{
		return Collections.unmodifiableSet(attachmentFormats);
	}

	/**
	 * @param attachmentFormats
	 *            the formats of the files the editors posting without a key attach, as their bytes
	 *            say: all those of {@link UploadFileFormat} by default
	 * @return {@code this}
	 */
	public RichTextFiles setAttachmentFormats(
		Collection<? extends IUploadFileFormat> attachmentFormats)
	{
		this.attachmentFormats = RichTextAttachmentUploads.checkedFormats(attachmentFormats);
		return this;
	}

	/**
	 * @return what scans each file uploaded before a store keeps it, {@code null} for none
	 */
	public IUploadScanner getScanner()
	{
		return scanner;
	}

	/**
	 * @param scanner
	 *            what scans each file uploaded here, image or file attached, before a store keeps
	 *            it, whichever editor uploads it: an antivirus, or a sanitizer of the application's
	 *            own; {@code null} for none, as by default
	 * @return {@code this}
	 */
	public RichTextFiles setScanner(IUploadScanner scanner)
	{
		this.scanner = scanner;
		return this;
	}

	/**
	 * @return the address the links of the files start with, {@code null} for the site's own
	 */
	public String getBaseUrl()
	{
		return baseUrl;
	}

	/**
	 * @param baseUrl
	 *            the address the links of the files start with, the names following it:
	 *            {@code https://files.example.com/rich-text-files} for a domain of their own, which
	 *            serves the same resource; {@code null} for the site's own, as by default
	 * @return {@code this}
	 */
	public RichTextFiles setBaseUrl(String baseUrl)
	{
		this.baseUrl = Strings.isEmpty(baseUrl) ? null : baseUrl.replaceAll("/+$", "");
		return this;
	}

	/**
	 * @return the application's check of who may see a file, {@code null} for none
	 */
	public BiPredicate<String, Request> getReadCheck()
	{
		return readCheck;
	}

	/**
	 * @param readCheck
	 *            whether the user of a request may see the file of a name, besides the store,
	 *            which opens only those the user may see: the session that uploaded it, the users
	 *            who may read the text it is in, for example; {@code null} for anyone with the
	 *            address, as by default. A file refused is not found.
	 * @return {@code this}
	 */
	public RichTextFiles setReadCheck(BiPredicate<String, Request> readCheck)
	{
		this.readCheck = readCheck;
		return this;
	}

	/**
	 * @return the application's check of who may upload, {@code null} for none
	 */
	public Predicate<Request> getUploadCheck()
	{
		return uploadCheck;
	}

	/**
	 * @param uploadCheck
	 *            whether the user of a request may upload, delete what they uploaded, keep it
	 *            permanently and find what was kept: those signed in, for example; {@code null} for
	 *            anyone, as by default, as the authorization strategy of the application allows
	 * @return {@code this}
	 */
	public RichTextFiles setUploadCheck(Predicate<Request> uploadCheck)
	{
		this.uploadCheck = uploadCheck;
		return this;
	}

	/**
	 * @return the address the editors of React and Svelte post their uploads to, the site's own,
	 *         from the root of the server: {@code uploadImageTo(url)} of the editor in the browser
	 */
	public String getUploadUrl()
	{
		return siteRoot() + path;
	}

	/**
	 * @return the address the editors of React and Svelte post the files they attach to:
	 *         {@code uploadFileTo(url)} of the editor in the browser
	 */
	public String getAttachmentUploadUrl()
	{
		return getUploadUrl() + "?" + KIND_PARAMETER + "=" + ATTACHMENTS;
	}

	/**
	 * @param name
	 *            the name of a file the store keeps, which it {@link IRichTextImageStore#open(String)
	 *            opens} by
	 * @return the address the file is served at, which the store answers for it: the site's own,
	 *         from the root of the server, or below {@link #setBaseUrl(String) the base address}
	 */
	public String addressOf(String name)
	{
		Args.notEmpty(name, "name");
		String encoded = UrlEncoder.PATH_INSTANCE.encode(name, StandardCharsets.UTF_8);
		return (baseUrl != null ? baseUrl : siteRoot() + path) + "/" + encoded;
	}

	/**
	 * @param url
	 *            an address
	 * @return whether it is that of a file of the resource, as {@link #addressOf(String)} answers
	 *         them: its address, or {@link #setBaseUrl(String) the base one}, a slash and a name
	 */
	public boolean isFileUrl(String url)
	{
		String prefix = (baseUrl != null ? baseUrl : siteRoot() + path) + "/";
		return url != null && url.length() > prefix.length() && url.startsWith(prefix);
	}

	/**
	 * @return the root of the application on the server, with a slash at its end
	 */
	private static String siteRoot()
	{
		Request request = RequestCycle.get().getRequest();
		String root = Strings.defaultIfEmpty(request.getContextPath(), "") +
			Strings.defaultIfEmpty(request.getFilterPath(), "");
		return root.endsWith("/") ? root : root + "/";
	}

	/**
	 * Registers the rules of an editor in the session, and answers the address its uploads go to.
	 *
	 * @param key
	 *            the key of the editor's uploads
	 * @param rules
	 *            its rules
	 * @param allowed
	 *            whether the editor takes uploads now: not while it is read only, for example
	 * @return the address, with the key
	 */
	String register(String key, RichTextImageUploads.Rules rules, boolean allowed)
	{
		Session session = Session.get();
		if (session.isTemporary())
		{
			session.bind();
		}
		Uploads uploads = uploads(session);
		uploads.register(path, key, capped(rules), allowed);
		session.setMetaData(UPLOADS, uploads);
		return getUploadUrl() + "?" + KEY_PARAMETER + "=" + key;
	}

	/**
	 * Registers the rules of the files an editor attaches in the session, and answers the address
	 * its files go to.
	 *
	 * @param allowed
	 *            whether the editor takes files now
	 * @return the address, with the key
	 */
	String registerAttachments(String key, RichTextAttachmentUploads.Rules rules, boolean allowed)
	{
		Session session = Session.get();
		if (session.isTemporary())
		{
			session.bind();
		}
		Uploads uploads = uploads(session);
		uploads.register(path, key + ATTACHMENT_SLOT, capped(rules), allowed);
		session.setMetaData(UPLOADS, uploads);
		return getUploadUrl() + "?" + KEY_PARAMETER + "=" + key + "&" + KIND_PARAMETER + "=" +
			ATTACHMENTS;
	}

	/**
	 * @return the rules of an editor, its files no larger than the resource takes, scanned by the
	 *         resource's scanner when it has none
	 */
	private RichTextAttachmentUploads.Rules capped(RichTextAttachmentUploads.Rules rules)
	{
		return new RichTextAttachmentUploads.Rules(rules.maxSize().greaterThan(attachmentMaxSize) ?
			attachmentMaxSize : rules.maxSize(), rules.formats(), rules.siteOnly(),
			rules.scanner() != null ? rules.scanner() : scanner);
	}

	/**
	 * @return a new key for the uploads of an editor
	 */
	static String newKey()
	{
		byte[] bytes = new byte[12];
		RANDOM.nextBytes(bytes);
		return HexFormat.of().formatHex(bytes);
	}

	/**
	 * @return the rules of an editor, its images no larger than the resource takes
	 */
	private RichTextImageUploads.Rules capped(RichTextImageUploads.Rules rules)
	{
		return new RichTextImageUploads.Rules(rules.maxSize().greaterThan(imageMaxSize) ?
			imageMaxSize : rules.maxSize(), rules.types(), rules.maxDimensions(), rules.siteOnly(),
			rules.permanent(), rules.maxFound(), rules.scanner() != null ? rules.scanner() :
				scanner);
	}

	/**
	 * @param key
	 *            the key of an editor's uploads
	 * @return the addresses of the images the editor took in this session and did not delete since
	 */
	Set<String> getUploaded(String key)
	{
		RichTextUploadedImages uploaded = uploaded(key);
		return uploaded == null ? Set.of() : uploaded.getUploaded();
	}

	/**
	 * Forgets images an editor took: they are no longer deleted when asked to.
	 */
	void forget(String key, Collection<String> urls)
	{
		RichTextUploadedImages uploaded = uploaded(key);
		if (uploaded != null)
		{
			uploaded.forget(urls);
			Session.get().setMetaData(UPLOADS, Session.get().getMetaData(UPLOADS));
		}
	}

	/**
	 * @return the addresses of the files the editor attached in this session and did not delete
	 *         since
	 */
	Set<String> getUploadedAttachments(String key)
	{
		return getUploaded(key + ATTACHMENT_SLOT);
	}

	/**
	 * Forgets files an editor attached: they are no longer deleted when asked to.
	 */
	void forgetAttachments(String key, Collection<String> urls)
	{
		forget(key + ATTACHMENT_SLOT, urls);
	}

	/**
	 * @return the images an editor took in this session, {@code null} for none registered
	 */
	private RichTextUploadedImages uploaded(String key)
	{
		if (!Session.exists())
		{
			return null;
		}
		Uploads uploads = Session.get().getMetaData(UPLOADS);
		Slot slot = uploads == null ? null : uploads.get(path, key);
		return slot == null ? null : slot.uploaded;
	}

	/**
	 * @return the uploads of the session, made the first time
	 */
	private static Uploads uploads(Session session)
	{
		synchronized (session)
		{
			Uploads uploads = session.getMetaData(UPLOADS);
			if (uploads == null)
			{
				uploads = new Uploads();
				session.setMetaData(UPLOADS, uploads);
			}
			return uploads;
		}
	}

	/**
	 * @return the rules of the editors posting without a key
	 */
	private RichTextImageUploads.Rules defaultRules()
	{
		return new RichTextImageUploads.Rules(imageMaxSize, imageTypes, imageMaxDimensions, false,
			permanentImages, maxFound, scanner);
	}

	/**
	 * @return the rules of the files of the editors posting without a key
	 */
	private RichTextAttachmentUploads.Rules defaultAttachmentRules()
	{
		return new RichTextAttachmentUploads.Rules(attachmentMaxSize, attachmentFormats, false,
			scanner);
	}

	/**
	 * Answers a POST of an editor: an upload, the images to delete or to keep permanently, or a
	 * search.
	 *
	 * @return the answer, {@code null} for a request refused
	 */
	private JSONObject answer(Request request)
	{
		HttpServletRequest http = (HttpServletRequest)request.getContainerRequest();
		Locale locale = Session.exists() ? Session.get().getLocale() : Locale.ENGLISH;
		if (!isSiteRequest(http))
		{
			LOG.warn("Refused a request of another site to the uploads at {}: {} {}", path,
				http.getHeader(FetchMetadataResourceIsolationPolicy.SEC_FETCH_SITE_HEADER),
				http.getHeader("Origin"));
			return null;
		}
		if (uploadCheck != null && !uploadCheck.test(request))
		{
			return null;
		}
		String key = request.getQueryParameters().getParameterValue(KEY_PARAMETER)
			.toString(DEFAULT_KEY);
		Session session = Session.get();
		if (session.isTemporary())
		{
			session.bind();
		}
		Uploads uploads = uploads(session);
		boolean attachments = ATTACHMENTS.equals(request.getQueryParameters()
			.getParameterValue(KIND_PARAMETER).toOptionalString());
		String id = attachments ? key + ATTACHMENT_SLOT : key;
		Slot slot;
		if (attachments && attachmentStore == null)
		{
			slot = null;
		}
		else if (DEFAULT_KEY.equals(key))
		{
			slot = uploads.defaultSlot(path, id, attachments ? defaultAttachmentRules() :
				defaultRules());
		}
		else
		{
			slot = uploads.get(path, id);
		}
		RichTextImageUploads.Texts texts = name -> RichTextEditor.textIn(locale, name);
		JSONObject answer;
		if (slot == null)
		{
			// the session of the editor is gone, the key made up, or no files are taken
			answer = new JSONObject().put("error", texts.text("uploadFailed").replace("{0}",
				texts.text(attachments ? "theFile" : "theImage")));
		}
		else if (slot.rules instanceof RichTextAttachmentUploads.Rules rules)
		{
			answer = RichTextAttachmentUploads.answer(request, rules, attachmentStore,
				slot.uploaded, slot.allowed, texts);
		}
		else
		{
			RichTextImageUploads.Rules images = (RichTextImageUploads.Rules)slot.rules;
			RichTextImageUploads.Rules rules = slot.allowed ? images :
				new RichTextImageUploads.Rules(images.maxSize(), images.types(),
					images.maxDimensions(), images.siteOnly(), false, images.maxFound(),
					images.scanner());
			answer = RichTextImageUploads.answer(request, rules, imageStore, slot.uploaded,
				slot.allowed, texts);
		}
		// the images taken go to the store of the sessions, in a cluster too
		session.setMetaData(UPLOADS, uploads);
		return answer;
	}

	/**
	 * @return whether the request comes from the site's own pages, as the browser tells by
	 *         {@code Sec-Fetch-Site}, else by {@code Origin}; a request telling neither is no
	 *         browser's, which carries no cookies of a user it was not sent by, and goes through
	 */
	private static boolean isSiteRequest(HttpServletRequest http)
	{
		for (IResourceIsolationPolicy policy : new IResourceIsolationPolicy[] {
				new FetchMetadataResourceIsolationPolicy(), new OriginResourceIsolationPolicy() })
		{
			ResourceIsolationOutcome outcome = policy.isRequestAllowed(http, null);
			if (outcome == ResourceIsolationOutcome.ALLOWED)
			{
				return true;
			}
			if (outcome == ResourceIsolationOutcome.DISALLOWED)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * @return the file of a name the store opens, which the user may see; {@code null} for none
	 */
	private RichTextServedFile find(String name, Request request)
	{
		if (!isName(name) || readCheck != null && !readCheck.test(name, request))
		{
			return null;
		}
		try
		{
			RichTextServedFile file = imageStore.open(name);
			return file == null && attachmentStore != null ? attachmentStore.open(name) : file;
		}
		catch (IOException | RuntimeException e)
		{
			LOG.warn("Could not open the file {} of the uploads at {}", name, path, e);
			return null;
		}
	}

	/**
	 * @return whether a name may be that of a file served: no path, no control character, not too
	 *         long
	 */
	static boolean isName(String name)
	{
		if (Strings.isEmpty(name) || name.length() > LONGEST_NAME || name.equals(".") ||
			name.equals(".."))
		{
			return false;
		}
		for (int i = 0; i < name.length(); i++)
		{
			char c = name.charAt(i);
			if (c == '/' || c == '\\' || Character.isISOControl(c))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * @param inline
	 *            whether a browser may open the file in its window
	 * @param fileName
	 *            the name of the file, the browser's when it was uploaded, {@code null} for none
	 * @return the value of {@code Content-Disposition}, the name as RFC 6266 says: in ASCII for the
	 *         browsers of old, and in UTF-8 by RFC 8187
	 */
	static String disposition(boolean inline, String fileName)
	{
		String type = inline ? "inline" : "attachment";
		String clean = fileName == null ? "" : fileName.replaceAll("\\p{Cntrl}", "").trim();
		clean = clean.substring(Math.max(clean.lastIndexOf('/'), clean.lastIndexOf('\\')) + 1)
			.trim();
		if (clean.isEmpty())
		{
			return type;
		}
		StringBuilder ascii = new StringBuilder();
		StringBuilder encoded = new StringBuilder();
		for (int i = 0; i < clean.length(); i++)
		{
			char c = clean.charAt(i);
			ascii.append(c >= 0x20 && c < 0x7f && c != '"' && c != '\\' && c != '%' ? c : '_');
		}
		for (byte b : clean.getBytes(StandardCharsets.UTF_8))
		{
			int c = b & 0xff;
			if (c < 0x80 && (Character.isLetterOrDigit(c) || "!#$&+-.^_`|~".indexOf(c) >= 0))
			{
				encoded.append((char)c);
			}
			else
			{
				encoded.append('%').append(HexFormat.of().withUpperCase().toHexDigits((byte)c));
			}
		}
		return type + "; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
	}

	/** The uploads of a session: the rules and the images of each editor, by path and key. */
	private static final class Uploads implements IClusterable
	{
		private static final long serialVersionUID = 1L;

		private final Map<String, Slot> slots = new LinkedHashMap<>();

		synchronized void register(String path, String key, IClusterable rules, boolean allowed)
		{
			String id = path + "\n" + key;
			Slot slot = slots.remove(id);
			slots.put(id, slot == null ? new Slot(rules, allowed) : slot.with(rules, allowed));
			Iterator<Map.Entry<String, Slot>> oldest = slots.entrySet().iterator();
			while (slots.size() > EDITORS && oldest.hasNext())
			{
				String each = oldest.next().getKey();
				if (each.endsWith("\n" + DEFAULT_KEY) ||
					each.endsWith("\n" + DEFAULT_KEY + ATTACHMENT_SLOT))
				{
					continue;
				}
				oldest.remove();
			}
		}

		synchronized Slot get(String path, String key)
		{
			return slots.get(path + "\n" + key);
		}

		synchronized Slot defaultSlot(String path, String key, IClusterable rules)
		{
			String id = path + "\n" + key;
			Slot slot = slots.get(id);
			slot = slot == null ? new Slot(rules, true) : slot.with(rules, true);
			slots.put(id, slot);
			return slot;
		}
	}

	/** The rules of an editor's uploads, and the images it took. */
	private static final class Slot implements IClusterable
	{
		private static final long serialVersionUID = 1L;

		/** The rules of images, or of files attached. */
		private final IClusterable rules;

		private final boolean allowed;

		private final RichTextUploadedImages uploaded;

		Slot(IClusterable rules, boolean allowed)
		{
			this(rules, allowed, new RichTextUploadedImages());
		}

		private Slot(IClusterable rules, boolean allowed, RichTextUploadedImages uploaded)
		{
			this.rules = rules;
			this.allowed = allowed;
			this.uploaded = uploaded;
		}

		/**
		 * @return the slot with other rules, the same images
		 */
		Slot with(IClusterable rules, boolean allowed)
		{
			return rules.equals(this.rules) && allowed == this.allowed ? this :
				new Slot(rules, allowed, uploaded);
		}
	}

	/** The resource answering the requests, the same for the whole application. */
	private static final class FilesResource extends AbstractResource
	{
		private static final long serialVersionUID = 1L;

		private final String path;

		FilesResource(String path)
		{
			this.path = path;
		}

		@Override
		protected ResourceResponse newResourceResponse(Attributes attributes)
		{
			RichTextFiles files = mounted(Application.get()).get(path);
			Request request = attributes.getRequest();
			HttpServletRequest http = (HttpServletRequest)request.getContainerRequest();
			String name = attributes.getParameters().get(NAME).toOptionalString();
			String method = http.getMethod();
			if (files == null)
			{
				return error(HttpServletResponse.SC_NOT_FOUND);
			}
			if ("POST".equalsIgnoreCase(method) && name == null)
			{
				return json(files.answer(request));
			}
			if (("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) &&
				name != null)
			{
				return serve(files.find(name, request), name, attributes);
			}
			return error(name == null ? HttpServletResponse.SC_METHOD_NOT_ALLOWED :
				HttpServletResponse.SC_NOT_FOUND);
		}

		private static ResourceResponse error(int code)
		{
			ResourceResponse response = new ResourceResponse();
			response.setError(code);
			response.disableCaching();
			return response;
		}

		/**
		 * @return the response of an answer as JSON, forbidden for none
		 */
		private static ResourceResponse json(JSONObject answer)
		{
			if (answer == null)
			{
				return error(HttpServletResponse.SC_FORBIDDEN);
			}
			byte[] bytes = answer.toString().getBytes(StandardCharsets.UTF_8);
			ResourceResponse response = new ResourceResponse();
			response.setContentType("application/json");
			response.setTextEncoding("UTF-8");
			response.setContentLength(bytes.length);
			response.disableCaching();
			response.setWriteCallback(new WriteCallback()
			{
				@Override
				public void writeData(Attributes attributes) throws IOException
				{
					attributes.getResponse().write(bytes);
				}
			});
			return response;
		}

		/**
		 * @return the response serving a file, not found for none
		 */
		private static ResourceResponse serve(RichTextServedFile file, String name,
			Attributes attributes)
		{
			if (file == null)
			{
				return error(HttpServletResponse.SC_NOT_FOUND);
			}
			IUploadFileFormat format = file.getFormat();
			FileResponse response = new FileResponse(disposition(format.isInline(),
				Strings.isEmpty(file.getFileName()) ? name : file.getFileName()));
			response.setContentType(format.getMimeType());
			if (format.getMimeType().startsWith("text/"))
			{
				response.setTextEncoding("UTF-8");
			}
			if (file.getSize() >= 0)
			{
				response.setContentLength(file.getSize());
			}
			response.setLastModified(file.getModified());
			response.setCacheDuration(CACHE);
			response.setCacheScope(WebResponse.CacheScope.PRIVATE);
			response.getHeaders().addHeader("X-Content-Type-Options", "nosniff");
			response.getHeaders().addHeader("Content-Security-Policy", "sandbox");
			if (response.dataNeedsToBeWritten(attributes))
			{
				response.setWriteCallback(new WriteCallback()
				{
					@Override
					public void writeData(Attributes attributes) throws IOException
					{
						try (InputStream input = file.getContent().open())
						{
							OutputStream output = attributes.getResponse().getOutputStream();
							input.transferTo(output);
						}
					}
				});
			}
			return response;
		}

		@Override
		protected void setResponseHeaders(ResourceResponse resourceResponse, Attributes attributes)
		{
			super.setResponseHeaders(resourceResponse, attributes);
			if (resourceResponse instanceof FileResponse file &&
				resourceResponse.getErrorCode() == null &&
				resourceResponse.dataNeedsToBeWritten(attributes) &&
				attributes.getResponse() instanceof WebResponse web)
			{
				// in place of Wicket's, as RFC 6266 says
				web.setHeader("Content-Disposition", file.disposition);
			}
		}
	}

	/** The response serving a file, with its disposition. */
	private static final class FileResponse extends AbstractResource.ResourceResponse
	{
		private final String disposition;

		FileResponse(String disposition)
		{
			this.disposition = disposition;
		}
	}
}
