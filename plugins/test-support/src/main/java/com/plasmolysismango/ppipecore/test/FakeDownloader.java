package com.plasmolysismango.ppipecore.test;

import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/** Downloader returning canned responses and recording every request. */
public final class FakeDownloader extends Downloader {

    public interface Handler {
        Response handle(Request request) throws IOException;
    }

    private final Map<String, Response> responses = new LinkedHashMap<>();
    private final List<Request> requests = new ArrayList<>();
    private Handler handler;

    public FakeDownloader respond(final String url, final String body) {
        return respond(url, 200, body, "text/html; charset=utf-8");
    }

    public FakeDownloader respond(final String url, final int status, final String body) {
        return respond(url, status, body, "text/html; charset=utf-8");
    }

    public FakeDownloader respond(final String url, final int status, final String body,
                                  final String contentType) {
        return respondBytes(url, status, body.getBytes(StandardCharsets.UTF_8), contentType);
    }

    public FakeDownloader respondBytes(final String url, final int status, final byte[] body,
                                       final String contentType) {
        final Map<String, List<String>> headers = new LinkedHashMap<>();
        if (contentType != null) {
            headers.put("Content-Type", Collections.singletonList(contentType));
        }
        responses.put(url, new Response(status, status == 200 ? "OK" : "Error", headers,
                new String(body, StandardCharsets.UTF_8), body, url));
        return this;
    }

    public FakeDownloader handler(final Handler handler) {
        this.handler = handler;
        return this;
    }

    public List<Request> requests() {
        return Collections.unmodifiableList(requests);
    }

    public Request lastRequest() {
        return requests.isEmpty() ? null : requests.get(requests.size() - 1);
    }

    @Nonnull
    @Override
    public Response execute(@Nonnull final Request request)
            throws IOException, ReCaptchaException {
        requests.add(request);
        final Response response = responses.get(request.url());
        if (response != null) {
            return response;
        }
        if (handler != null) {
            final Response handled = handler.handle(request);
            if (handled != null) {
                return handled;
            }
        }
        throw new IOException("FakeDownloader has no stub for " + request.url());
    }

    @Override
    public CancellableCall executeAsync(@Nonnull final Request request,
                                        @Nonnull final AsyncCallback callback)
            throws IOException, ReCaptchaException {
        callback.onSuccess(execute(request));
        return null;
    }
}
