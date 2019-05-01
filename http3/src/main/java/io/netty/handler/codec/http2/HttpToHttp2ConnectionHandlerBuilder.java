/*
 * Copyright 2015 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.netty.handler.codec.http2;

import io.netty.handler.codec.http2.Http2HeadersEncoder.SensitivityDetector;
import io.netty.util.internal.UnstableApi;

/**
 * Builder which builds {@link HttpToHttp2ConnectionHandler} objects.
 */
@UnstableApi
public final class HttpToHttp2ConnectionHandlerBuilder extends
        AbstractHttp2ConnectionHandlerBuilder<HttpToHttp2ConnectionHandler, HttpToHttp2ConnectionHandlerBuilder> {

    @Override
    public HttpToHttp2ConnectionHandlerBuilder validateHeaders(final boolean validateHeaders) {
        return super.validateHeaders(validateHeaders);
    }

    @Override
    public HttpToHttp2ConnectionHandlerBuilder initialSettings(final Http2Settings settings) {
        return super.initialSettings(settings);
    }

    @Override
    public HttpToHttp2ConnectionHandlerBuilder frameListener(final Http2FrameListener frameListener) {
        return super.frameListener(frameListener);
    }

    @Override
    public HttpToHttp2ConnectionHandlerBuilder gracefulShutdownTimeoutMillis(final long gracefulShutdownTimeoutMillis) {
        return super.gracefulShutdownTimeoutMillis(gracefulShutdownTimeoutMillis);
    }

    @Override
    public HttpToHttp2ConnectionHandlerBuilder server(final boolean isServer) {
        return super.server(isServer);
    }

    @Override
    public HttpToHttp2ConnectionHandlerBuilder connection(final Http2Connection connection) {
        return super.connection(connection);
    }

    @Override
    public HttpToHttp2ConnectionHandlerBuilder codec(final Http2ConnectionDecoder decoder,
                                                     final Http2ConnectionEncoder encoder) {
        return super.codec(decoder, encoder);
    }

    @Override
    public HttpToHttp2ConnectionHandlerBuilder frameLogger(final Http2FrameLogger frameLogger) {
        return super.frameLogger(frameLogger);
    }

    @Override
    public HttpToHttp2ConnectionHandlerBuilder encoderEnforceMaxConcurrentStreams(
            final boolean encoderEnforceMaxConcurrentStreams) {
        return super.encoderEnforceMaxConcurrentStreams(encoderEnforceMaxConcurrentStreams);
    }

    @Override
    public HttpToHttp2ConnectionHandlerBuilder headerSensitivityDetector(
            final SensitivityDetector headerSensitivityDetector) {
        return super.headerSensitivityDetector(headerSensitivityDetector);
    }

    @Override
    public HttpToHttp2ConnectionHandlerBuilder initialHuffmanDecodeCapacity(final int initialHuffmanDecodeCapacity) {
        return super.initialHuffmanDecodeCapacity(initialHuffmanDecodeCapacity);
    }

    @Override
    public HttpToHttp2ConnectionHandlerBuilder decoupleCloseAndGoAway(final boolean decoupleCloseAndGoAway) {
        return super.decoupleCloseAndGoAway(decoupleCloseAndGoAway);
    }

    @Override
    public HttpToHttp2ConnectionHandler build() {
        return super.build();
    }

    @Override
    protected HttpToHttp2ConnectionHandler build(final Http2ConnectionDecoder decoder, final Http2ConnectionEncoder encoder,
                                                 final Http2Settings initialSettings) {
        return new HttpToHttp2ConnectionHandler(decoder, encoder, initialSettings, isValidateHeaders(),
                decoupleCloseAndGoAway());
    }
}
