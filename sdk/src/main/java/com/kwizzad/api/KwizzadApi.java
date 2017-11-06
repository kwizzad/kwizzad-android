package com.kwizzad.api;

import com.kwizzad.db.DB;
import com.kwizzad.db.FromJson;
import com.kwizzad.db.ToJson;
import com.kwizzad.log.QLog;
import com.kwizzad.model.Model;
import com.kwizzad.model.events.AEvent;
import com.kwizzad.model.events.EventLookup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class KwizzadApi {

    private static final int MAX_RETRIES_COUNT = 10;

    private static KwizzadApi instance;

    private Model model;
    private boolean logHeaders = true;

    private int[] retryIntervals = {1, 5, 10, 60, 360, 1440, 1440, 1440, 1440, 1440};


    private final PublishSubject<AEvent> eventsSubject = PublishSubject.create();

    private KwizzadApi() {
        // Exists only to defeat instantiation.
    }

    public static KwizzadApi getInstance() {
        if(instance == null) {
            instance = new KwizzadApi();
        }
        return instance;
    }

    public <T extends AEvent> Observable<T> observe(Class<T> clazz) {
        return eventsSubject
                .filter((event) -> {
                    if (event.getClass().equals(clazz)) {
                        return true;
                    }
                    return false;
                })
                .map(event -> (T) event);
    }

    public Observable<AEvent> send(Object event) {
        return send(Arrays.asList(event));
    }

    public static Object convert(Object data) {
        if (data instanceof Collection) {
            JSONArray arr = new JSONArray();
            for (Object el : ((Collection) data)) {
                arr.put(convert(el));
            }
            return arr.toString();
        } else if (data != null) {

            try {
                Class type = data.getClass();
                final List<Method> methods = DB.getMethodsAnnotatedWith(type, ToJson.class);
                for (Method m : methods) {
                    if (!Modifier.isStatic(m.getModifiers())) {
                        if (m.getParameterTypes().length == 1 && m.getParameterTypes()[0].isAssignableFrom(JSONObject.class)) {
                            JSONObject o = new JSONObject();
                            m.invoke(data, o);
                            return o;
                        }
                    }
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
                return null;
            }

            // fallback
            return data.toString();
        }
        return null;
    }

    public static <T> T convert(JSONObject jsonObject, Class<T> type) throws Exception {
        for (Method m : DB.getMethodsAnnotatedWith(type, FromJson.class)) {
            if (
                    m.getParameterTypes().length == 1
                            && !Modifier.isStatic(m.getModifiers())
                            && m.getParameterTypes()[0].isAssignableFrom(JSONObject.class)
                    ) {

                T o = type.newInstance();

                m.invoke(o, jsonObject);

                return o;

            }
        }
        return null;
    }

    public Observable<AEvent> send(List<Object> event) {
        return Observable.fromCallable(
                () -> {
                    final String str = convert(event).toString();
                    final String apiKey = model.getApiKey();
                    Request.Builder request = new Request.Builder();
                    request.method("POST", RequestBody.create("application/json; charset=UTF-8", str));
                    request.url(model.getBaseUrl(apiKey) + apiKey + "/" + model.getApiKey());

                    Request r = request.build();
                    log(r, str);
                    return r;
                })
                .flatMap(this::send)
                .flatMap(this::isValidResponse)
                .retryWhen(errors -> errors
                        .zipWith(Observable.range(0, MAX_RETRIES_COUNT), (n, retryIndex) -> {
                            QLog.e("error sending request: " + n.getMessage());
                            return retryIndex;
                        })
                        .flatMap(retryIndex -> {
                            Random random = new Random(System.currentTimeMillis());
                            int timeWithRandomPart = (int) (retryIntervals[retryIndex] + retryIntervals[retryIndex] * (random.nextFloat() - 0.5));
                            QLog.i("retry after: " + timeWithRandomPart);
                            return Observable.timer(timeWithRandomPart, TimeUnit.MINUTES);
                        }))
                .flatMap(response -> {
                    try {

                        if(response.code() < 499 && response.code() >= 400) {
                            return Observable.error(new Exception(response.message()));
                        }

                        String rr = response.body();

                        log(response, rr);

                        ArrayList<AEvent> events = new ArrayList<AEvent>();
                        JSONArray arr = new JSONArray(rr);
                        JSONObject o;
                        AEvent e;
                        Class<? extends AEvent> type;
                        for (int ii = 0; ii < arr.length(); ii++) {
                            o = arr.getJSONObject(ii);
                            try {
                                type = EventLookup.get(o.getString("type"));
                                if (null != type) {
                                    e = convert(o, type);
                                    if (e != null) {
                                        eventsSubject.onNext(e);
                                        events.add(e);
                                    }
                                }
                            } catch (Exception ex) {
                                QLog.e(ex);
                            }
                        }

                        return Observable.fromIterable(events);
                    } catch (Exception ignored) {
                        QLog.v(ignored);
                    } finally {
                        //if (response.body() != null)
                        //    response.body().close();
                    }
                    return Observable.empty();
                });
    }

    public Observable<Response> isValidResponse(Response response) {
        if (response != null) {
            try {
                // 499 is nginx-y for a backend timeout, 500+ is reserved for server-side errors.
                // We regard these as retry-able because probably we just have to wait for a backend
                // to be available again later.
                // Response errors < 499 mean errors on our side, so we won't retry the according requests.
                if (response.code() < 499) {
                    return Observable.just(response);
                } else {
                    return Observable.error(new HttpErrorResponseException(response.code(), response.message()));
                }
            } catch (Throwable e) {
                return Observable.error(e);
            }
        } else {
            return Observable.error(new Exception("no response"));
        }
    }

    public Observable<Response> send(Request request) {
        try {

            URL url = new URL(request.url());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            for (Map.Entry<String, String> entry : request.headers().entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            connection.setRequestMethod(request.method());

            if (request.body() != null) {
                connection.setDoOutput(true);
                OutputStream os = connection.getOutputStream();
                os.write(request.body());
                os.flush();
                os.close();
            }

            int responseCode = connection.getResponseCode();

            StringBuilder sb = new StringBuilder();

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), Charset.forName("UTF-8")));
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
            } catch (Exception e) {
                //QLog.d(e, "error reading");
            }

            return Observable.just(new Response(request, responseCode, connection.getResponseMessage(), connection.getHeaderFields(), sb.length() > 0 ? sb.toString() : null));

        } catch (Throwable throwable) {

            return Observable.error(throwable);
        }
    }

    private void log(Response request, boolean _logHeaders) {
        log(request, _logHeaders, null);
    }

    private void log(Response request) {
        log(request, logHeaders, null);
    }

    private void log(Response request, String body) {
        log(request, logHeaders, body);
    }

    private void log(Response response, boolean _logHeaders, String body) {
        try {
            StringBuilder sb = new StringBuilder();
            String tab = "     ";
            String newline = "\n";
            boolean clog = _logHeaders || body != null;

            if (clog) {
                sb.append(newline);
                sb.append(newline);
            }

            sb.append("<--- ")
                    .append(response.code())
                    .append(" ")
                    .append(response.message())
                    .append(" ")
                    .append(response.request().url());

            sb.append(newline);

            if (_logHeaders)
                for (Map.Entry<String, List<String>> entry : response.headers().entrySet()) {
                    for (String value : entry.getValue()) {
                        sb.append(tab);
                        sb.append(entry.getKey());
                        sb.append(" : ");
                        sb.append(value);
                        sb.append(newline);
                    }
                }

            if (body != null) {
                sb.append(newline);
                sb.append(body);
                sb.append(newline);

            }

            if (clog) {
                sb.append(" ");
            }

            QLog.d(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void log(Request request, String body) {
        log(request, logHeaders, body);
    }

    private void log(Request request, boolean _logHeaders, String body) {
        try {
            StringBuilder sb = new StringBuilder();
            String tab = "     ";
            String newline = "\n";

            if (_logHeaders) {
                sb.append(newline);
                sb.append(newline);
            }
            sb.append("---> ")
                    .append(request.method())
                    .append(" ")
                    .append(request.url());

            sb.append(newline);

            if (_logHeaders)
                for (Map.Entry<String, String> entry : request.headers().entrySet()) {
                    sb.append(tab);
                    sb.append(entry.getKey());
                    sb.append(" : ");
                    sb.append(entry.getValue());
                    sb.append(newline);
                }


            try {
                if (body != null) {
                    sb.append(newline);

                    sb.append(body);

                    sb.append(newline);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (_logHeaders) {
                sb.append(" ");
            }

            QLog.d(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setModel(Model model) {
        this.model = model;
    }

    public void init(Model model) {
        this.model = model;
    }
}
