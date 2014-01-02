// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.darwinathome.body.Target;
import org.darwinathome.genetics.Genome;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.network.CargoCatcher;
import org.darwinathome.network.Exchange;
import org.darwinathome.network.Failure;
import org.darwinathome.network.Hub;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * The universe on the client side
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class HttpHub implements Hub {
    private Executor executor = Executors.newSingleThreadExecutor();
    private Logger log = Logger.getLogger(getClass());
    private HttpClient httpClient;
    private String baseUrl;
    private String session = Hub.NO_SESSION;

    public HttpHub(String baseUrl) {
        this.httpClient = new DefaultHttpClient();
        this.baseUrl = baseUrl;
    }

    private abstract class ExchangeRunner implements Runnable {
        private String serviceName;
        private Exchange exchange;
        private CargoCatcher cargoCatcher;

        private ExchangeRunner(String serviceName, Exchange exchange, CargoCatcher cargoCatcher) {
            this.serviceName = serviceName;
            this.exchange = exchange;
            this.cargoCatcher = cargoCatcher;
        }

        private ExchangeRunner(String serviceName, Exchange exchange) {
            this(serviceName, exchange, null);
        }

        public void run() {
            String methodUrl = baseUrl + serviceName.replace("{session}", session);
            HttpRequestBase request = getRequest(methodUrl);
            try {
                log.info("executing " + methodUrl);
                HttpResponse httpResponse = httpClient.execute(request);
                log.info("response " + methodUrl);
                switch (httpResponse.getStatusLine().getStatusCode()) {
                    case HttpStatus.SC_OK:
                        if (cargoCatcher != null) {
                            DataInputStream dis = new DataInputStream(httpResponse.getEntity().getContent());
                            String response = dis.readUTF();
                            if (response.startsWith(Hub.SUCCESS)) {
                                log.info(methodUrl + " cargo received");
                                try {
                                    cargoCatcher.catchCargo(dis);
                                    exchange.success();
                                }
                                catch (IOException e) {
                                    exchange.fail(Failure.MARSHALLING);
                                    log.warn("Problem catching cargo", e);
                                }
                            }
                            else if (response.startsWith(Hub.FAILURE)) {
                                log.info(methodUrl + " failure");
                                Failure failure = Failure.valueOf(dis.readUTF());
                                exchange.fail(failure);
                            }
                            else {
                                log.warn("Didn't recognize response: " + response);
                                exchange.fail(Failure.NETWORK_CONNECTON);
                            }
                        }
                        else {
                            HttpEntity entity = httpResponse.getEntity();
                            String response = EntityUtils.toString(entity);
                            if (response.startsWith(Hub.SUCCESS)) {
                                log.info(methodUrl + " success");
                                exchange.success();
                            }
                            else if (response.startsWith(Hub.FAILURE)) {
                                log.info(methodUrl + " failure");
                                String failureString = response.substring(Hub.FAILURE.length()).trim();
                                Failure failure = Failure.valueOf(failureString);
                                exchange.fail(failure);
                            }
                            else {
                                log.warn("Didn't recognize response: " + response);
                                exchange.fail(Failure.NETWORK_CONNECTON);
                            }
                        }
                        break;
                    default:
                        log.warn(httpResponse.getStatusLine().getReasonPhrase());
                        exchange.fail(Failure.NETWORK_CONNECTON);
                        break;
                }
            }
            catch (IOException e) {
                log.warn("Communications IO", e);
                exchange.fail(Failure.NETWORK_CONNECTON);
            }
            finally {
                log.info("finally " + methodUrl);
                request.abort();
            }
        }

        abstract HttpRequestBase getRequest(String serviceUrl);
    }

    private void submit(Runnable runnable) {
        executor.execute(runnable);
    }

    public void authenticate(final String email, final String password, final Exchange exchange) {
        CargoCatcher sessionCatcher = new CargoCatcher() {
            public void catchCargo(DataInputStream dis) throws IOException {
                session = dis.readUTF();
            }
        };
        submit(new ExchangeRunner(Hub.AUTHENTICATE_SERVICE, exchange, sessionCatcher) {
            @Override
            HttpRequestBase getRequest(String serviceUrl) {
                return createPost(serviceUrl,
                        Hub.PARAM_BODY_NAME, email,
                        Hub.PARAM_PASSWORD, password
                );
            }
        });
    }

    public void getWorld(CargoCatcher catcher, Exchange exchange) {
        submit(new ExchangeRunner(Hub.GET_WORLD_SERVICE, exchange, catcher) {
            @Override
            HttpRequestBase getRequest(String serviceUrl) {
                return createPost(serviceUrl);
            }
        });
    }

    public void createBeing(final Arrow location, final Exchange exchange) {
        submit(new ExchangeRunner(Hub.CREATE_BEING_SERVICE, exchange) {
            @Override
            HttpRequestBase getRequest(String serviceUrl) {
                return createPost(serviceUrl,
                        Hub.PARAM_LOCATION_X, String.valueOf(location.x),
                        Hub.PARAM_LOCATION_Y, String.valueOf(location.y),
                        Hub.PARAM_LOCATION_Z, String.valueOf(location.z)
                );
            }
        });
    }

    public void setSpeech(final String speech, final Exchange exchange) {
        submit(new ExchangeRunner(Hub.SET_SPEECH_SERVICE, exchange) {
            @Override
            HttpRequestBase getRequest(String serviceUrl) {
                return createPost(serviceUrl,
                        Hub.PARAM_SPEECH, speech
                );
            }
        });
    }

    public void getSpeechSince(final long time, CargoCatcher catcher, Exchange exchange) {
        submit(new ExchangeRunner(Hub.GET_SPEECH_SINCE_SERVICE, exchange, catcher) {
            @Override
            HttpRequestBase getRequest(String serviceUrl) {
                return createPost(serviceUrl,
                        Hub.PARAM_TIME, String.valueOf(time)
                );
            }
        });
    }

    public void setGenome(final Genome genome, final Exchange exchange) {
        submit(new ExchangeRunner(Hub.SET_GENOME_SERVICE, exchange) {
            @Override
            HttpRequestBase getRequest(String serviceUrl) {
                HttpPost post = new HttpPost(serviceUrl);
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    genome.write(dos);
                    dos.close();
                    ByteArrayEntity entity = new ByteArrayEntity(baos.toByteArray());
                    entity.setContentType("application/octet-stream");
                    post.setEntity(entity);
                }
                catch (IOException e) {
                    log.warn("Problem marshalling", e);
                    exchange.fail(Failure.MARSHALLING);
                }
                return post;
            }
        });
    }

    public void setTarget(final Target target, CargoCatcher catcher, final Exchange exchange) {
        submit(new ExchangeRunner(Hub.SET_TARGET_SERVICE, exchange, catcher) {
            @Override
            HttpRequestBase getRequest(String serviceUrl) {
                return createPost(serviceUrl,
                        Hub.PARAM_LOCATION_X, String.valueOf(target.getLocation().x),
                        Hub.PARAM_LOCATION_Y, String.valueOf(target.getLocation().y),
                        Hub.PARAM_LOCATION_Z, String.valueOf(target.getLocation().z),
                        Hub.PARAM_PREY_NAME, target.getPreyName()
                );
            }
        });
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    private HttpPost createPost(String serviceUrl, String... params) {
        try {
            HttpPost post = new HttpPost(serviceUrl);
            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            for (int walk = 0; walk < params.length; walk += 2) {
                pairs.add(new BasicNameValuePair(params[walk], params[walk + 1]));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs, "UTF-8");
            post.setEntity(entity);
            return post;
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}