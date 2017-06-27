package com.kwizzad.api;

import com.kwizzad.ISchedulers;
import com.kwizzad.db.DB;
import com.kwizzad.db.FromJson;
import com.kwizzad.db.ToJson;
import com.kwizzad.log.QLog;
import com.kwizzad.model.Model;
import com.kwizzad.model.events.AEvent;
import com.kwizzad.model.events.EventLookup;
import org.json.JSONArray;
import org.json.JSONObject;
import rx.Observable;
import rx.subjects.PublishSubject;

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

public class KwizzadApi {

    private final Model model;
    ISchedulers schedulers;
    private boolean logHeaders = true;

    private final PublishSubject<AEvent> eventsSubject = PublishSubject.create();

    public KwizzadApi(ISchedulers schedulers, Model model) {
        this.schedulers = schedulers;
        this.model = model;
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
                    Request.Builder request = new Request.Builder();
                    request.method("POST", RequestBody.create("application/json; charset=UTF-8", str));
                    request.url(model.server + model.apiKey.get() + "/" + model.installId.get());

                    Request r = request.build();
                    log(r, str);
                    return r;
                })
                .flatMap(this::send)
                .flatMap(this::isValidResponse)
                .flatMap(response -> {
                    try {

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

                        return Observable.from(events);
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
                if (response.isSuccessful()) {

                    return Observable.just(response);

                } else {
                    //if (response.body() != null)
                    //    response.body().close();

                    return Observable.error(new HttpErrorResponseException(response.code(), response.message()));
                }
            } catch (Throwable e) {

                //if (response.body() != null)
                //    response.body().close();

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

            if(responseCode>=499 && responseCode<=599) {

                return Observable.error(new HttpErrorResponseException(responseCode, connection.getResponseMessage()));

            }
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
}
