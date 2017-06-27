package com.kwizzad.model;

public class Components {
    /*public static boolean updateFrom(AComponentModel componentModel, JSONObject o) throws JSONException {
        if (o.isNull("__components__") == false) {
            JSONArray arr = o.getJSONArray("__components__");
            for (int ii = 0, len = arr.length(); ii < len; ii++) {
                JSONObject cjson = arr.getJSONObject(ii);
                try {
                    Class<? extends IComponent> clazz = (Class<? extends IComponent>)Class.forName(cjson.getString("__class__"));
                    IComponent c = clazz.newInstance();
                    if(c.updateFrom(cjson)) {
                        componentMap.put(clazz,c);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    public boolean toJSON(AComponentModel componentModel,JSONObject o) throws JSONException {

        JSONArray arr = new JSONArray();
        for (Map.Entry<Class<? extends IComponent>, IComponent> entry : componentMap.entrySet()) {
            JSONObject cjson = new JSONObject();
            cjson.put("__class__", entry.getKey().getCanonicalName());
            entry.getValue().toJSON(cjson);
            arr.put(cjson);
        }
        if (arr.length() > 0)
            o.put("__components__", arr);


        return true;
    }*/
}
