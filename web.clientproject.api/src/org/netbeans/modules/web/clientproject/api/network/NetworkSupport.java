/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.web.clientproject.api.network;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import org.netbeans.api.annotations.common.CheckReturnValue;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.modules.web.clientproject.api.network.ui.NetworkErrorPanel;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Cancellable;
import org.openide.util.NbBundle;
import org.openide.util.Pair;
import org.openide.util.Parameters;

/**
 * Helper class for network issues and tasks.
 * @since 1.13
 */
public final class NetworkSupport {

    private static final Logger LOGGER = Logger.getLogger(NetworkSupport.class.getName());


    private NetworkSupport() {
    }

    /**
     * Show network error dialog with possibility to retry the request
     * or open general IDE options where proxy can be configured.
     * <p>
     * See {@link #showNetworkErrorDialog(List)} for more information.
     * @param failedRequest request that failed, never {@code null}
     * @return {@code true} if the request should be downloaded once more, {@code false} otherwise
     * @see #showNetworkErrorDialog(List)
     */
    @CheckReturnValue
    public static boolean showNetworkErrorDialog(String failedRequest) {
        Parameters.notNull("failedRequest", failedRequest); // NOI18N
        return showNetworkErrorDialog(Collections.singletonList(failedRequest));
    }

    /**
     * Show network error dialog with possibility to retry the requests
     * or open general IDE options where proxy can be configured.
     * <p>
     * Notes:
     * <ul>
     *   <li>If the request is URL (starts with "http://" or "https://"), it is
     *       displayed as a hyperlink.</li>
     *   <li>If the request is longer than {@value NetworkErrorPanel#MAX_REQUEST_LENGTH}
     *       characters, it is truncated (using "...").</li>
     * </ul>
     * @param failedRequests requests that failed, never {@code null}
     * @return {@code true} if the requests should be downloaded once more, {@code false} otherwise
     * @see #showNetworkErrorDialog(String)
     */
    @NbBundle.Messages({
        "NetworkSupport.errorDialog.title=Network error",
        "# {0} - failed URLs",
        "NetworkSupport.errorDialog.text=<html>Network error occured while processing these requests:<br><br>{0}<br><br>Try it again?",
        "NetworkSupport.errorDialog.configureProxy=Configure Proxy..."
    })
    @CheckReturnValue
    public static boolean showNetworkErrorDialog(List<String> failedRequests) {
        Parameters.notNull("failedRequests", failedRequests); // NOI18N
        if (failedRequests.isEmpty()) {
            throw new IllegalArgumentException("Failed requests must be provided.");
        }
        DialogDescriptor descriptor = new DialogDescriptor(
                new NetworkErrorPanel(failedRequests),
                Bundle.NetworkSupport_errorDialog_title(),
                true,
                DialogDescriptor.YES_NO_OPTION,
                DialogDescriptor.YES_OPTION,
                null);
        JButton configureProxyButton = new JButton(Bundle.NetworkSupport_errorDialog_configureProxy());
        configureProxyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                OptionsDisplayer.getDefault().open(OptionsDisplayer.GENERAL);
            }
        });
        descriptor.setAdditionalOptions(new Object[] {configureProxyButton});
        return DialogDisplayer.getDefault().notify(descriptor) == NotifyDescriptor.YES_OPTION;
    }

    /**
     * Download the given URL to the target file.
     * <p>
     * This method must be called only in a background thread. To cancel the download, interrupt the current thread.
     * @param url URL to be downloaded
     * @param target target file
     * @throws NetworkException if any network error occurs
     * @throws IOException if any error occurs
     * @throws InterruptedException if the download is cancelled
     * @see #downloadWithProgress(java.lang.String, java.io.File, java.lang.String)
     * @see #downloadWithProgress(java.lang.String, java.io.File, org.netbeans.api.progress.ProgressHandle)
     * @since 1.25
     */
    public static void download(@NonNull String url, @NonNull File target) throws NetworkException, IOException, InterruptedException {
        downloadInternal(url, target, null);
    }

    /**
     * Download the given URL to the target file with showing its progress.
     * <p>
     * This method must be called only in a background thread. To cancel the download, interrupt the current thread.
     * @param url URL to be downloaded
     * @param target target file
     * @param displayName display name of the progress
     * @throws NetworkException if any network error occurs
     * @throws IOException if any error occurs
     * @throws InterruptedException if the download is cancelled
     * @see #download(java.lang.String, java.io.File)
     * @see #downloadWithProgress(java.lang.String, java.io.File, org.netbeans.api.progress.ProgressHandle)
     * @since 1.25
     */
    public static void downloadWithProgress(@NonNull String url, @NonNull File target, @NonNull String displayName)
            throws NetworkException, IOException, InterruptedException {
        Parameters.notNull("displayName", displayName);
        final Thread downloadThread = Thread.currentThread();
        ProgressHandle progressHandle = ProgressHandle.createHandle(displayName, new Cancellable() {
            @Override
            public boolean cancel() {
                downloadThread.interrupt();
                return true;
            }
        });
        progressHandle.start();
        try {
            downloadInternal(url, target, progressHandle);
        } finally {
            progressHandle.finish();
        }
    }

    /**
     * Download the given URL to the target file with showing its progress
     * using the given, <b>already started</b> progress handle. Such progress handle is
     * neither started nor finished.
     * <p>
     * This method must be called only in a background thread. To cancel the download, interrupt the current thread.
     * @param url URL to be downloaded
     * @param target target file
     * @param progressHandle existing, <b>already started</b> progress handle
     * @throws NetworkException if any network error occurs
     * @throws IOException if any error occurs
     * @throws InterruptedException if the download is cancelled
     * @see #download(java.lang.String, java.io.File)
     * @see #downloadWithProgress(java.lang.String, java.io.File, java.lang.String)
     * @since 1.25
     */
    public static void downloadWithProgress(@NonNull String url, @NonNull File target, @NonNull ProgressHandle progressHandle)
            throws NetworkException, IOException, InterruptedException {
        Parameters.notNull("progressHandle", progressHandle);
        downloadInternal(url, target, progressHandle);
    }

    @NbBundle.Messages({
        "# {0} - file name",
        "NetworkSupport.progress.download=Downloading {0}",
    })
    private static void downloadInternal(@NonNull String url, @NonNull File target, @NullAllowed ProgressHandle progressHandle)
            throws NetworkException, IOException, InterruptedException {
        Parameters.notNull("url", url);
        Parameters.notNull("target", target);
        if (EventQueue.isDispatchThread()) {
            throw new IllegalStateException("Cannot run in UI thread");
        }
        Pair<InputStream, Integer> downloadSetup = prepareDownload(url, progressHandle);
        checkInterrupted();
        doDownload(url, target, downloadSetup, progressHandle);
    }

    @NbBundle.Messages("NetworkSupport.progress.prepare=Preparing download...")
    private static Pair<InputStream, Integer> prepareDownload(String url, @NullAllowed ProgressHandle progressHandle) throws NetworkException, InterruptedException {
        try {
            int contentLength = -1;
            URL resource = new URL(url);
            if (progressHandle != null) {
                progressHandle.switchToIndeterminate();
                progressHandle.progress(Bundle.NetworkSupport_progress_prepare());
                contentLength = getContentLength(resource);
                if (contentLength != -1) {
                    progressHandle.switchToDeterminate(contentLength);
                }
            }
            checkInterrupted();
            return Pair.of(resource.openStream(), contentLength);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, null, ex);
            throw new NetworkException(url, ex);
        }
    }

    private static int getContentLength(URL url) throws IOException, InterruptedException {
        checkInterrupted();
        URLConnection urlConnection = url.openConnection();
        int length = urlConnection.getContentLength();
        if (length != -1) {
            return length;
        }
        // fallback
        if (urlConnection instanceof HttpURLConnection) {
            checkInterrupted();
            HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setRequestMethod("HEAD"); // NOI18N
            InputStream inputStream = httpUrlConnection.getInputStream();
            try {
                return httpUrlConnection.getContentLength();
            } finally {
                inputStream.close();
            }
        }
        return -1;
    }

    private static void doDownload(String url, File target, Pair<InputStream, Integer> downloadSetup, @NullAllowed ProgressHandle progressHandle)
            throws IOException, InterruptedException {
        if (progressHandle != null) {
            progressHandle.progress(Bundle.NetworkSupport_progress_download(url));
        }
        try (InputStream is = downloadSetup.first()) {
            copyToFile(is, target, progressHandle, downloadSetup.second());
        } catch (IOException ex) {
            // error => ensure file is deleted
            if (!target.delete()) {
                // nothing we can do about it
            }
            throw ex;
        }
    }

    private static File copyToFile(InputStream is, File target, @NullAllowed ProgressHandle progressHandle, int contentLength) throws IOException, InterruptedException {
        OutputStream os = new FileOutputStream(target);
        try {
            final byte[] buffer = new byte[65536];
            int len;
            int read = 0;
            for (;;) {
                checkInterrupted();
                len = is.read(buffer);
                if (len == -1) {
                    break;
                }
                os.write(buffer, 0, len);
                if (contentLength != -1) {
                    assert progressHandle != null;
                    read += len;
                    progressHandle.progress(read);
                }
            }
        } finally {
            os.close();
        }
        return target;
    }

    private static void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }

}
