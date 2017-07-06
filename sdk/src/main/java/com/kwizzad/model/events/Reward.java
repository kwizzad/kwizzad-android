package com.kwizzad.model.events;

import android.content.Context;
import android.text.TextUtils;

import com.kwizzad.R;
import com.kwizzad.log.QLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;

public class Reward {
    public final int amount;
    public final int maxAmount;
    public final String currency;
    public final Type type;

    public Reward(JSONObject vo) {
        amount = vo.optInt("amount", 0);
        maxAmount = vo.optInt("maxAmount", 0);
        currency = vo.optString("currency");
        type = Type.from(vo.optString("type"));
    }

    public Reward(int amount, int maxAmount, String currency, Type type) {
        this.amount = amount;
        this.maxAmount = maxAmount;
        this.currency = currency;
        this.type = type;
    }

    public enum Type {
        CALLBACK("callback"),
        CALL2ACTIONSTARTED("call2ActionStarted"),
        GOALREACHED("goalReached"),
        UNKNOWN("");

        public final String key;

        Type(String key) {
            this.key = key;
        }

        public static final Type from(String key) {
            if (key != null) {
                for (Type type : Type.values()) {
                    if (type.key.equalsIgnoreCase(key)) {
                        return type;
                    }
                }
            }
            return UNKNOWN;
        }
    }

    public static Reward from(JSONObject vo) throws JSONException {
        if (vo != null) {
            return new Reward(vo);
        }
        return null;
    }

    public static List<Reward> fromArray(JSONArray rewards) {
        List<Reward> ret = new ArrayList<>();
        for (int ii = 0; ii < rewards.length(); ii++) {
            try {
                ret.add(Reward.from(rewards.getJSONObject(ii)));
            } catch (Exception e) {
                QLog.d(e);
            }
        }
        return ret;

    }

    @Override
    public String toString() {
        return "Reward (" + type + ": " + amount + "," + currency + ")";
    }

    public String valueDescription(Context context) {
        String valueDescription;
        if(maxAmount > 0) {
            valueDescription = context.getString(R.string.reward_withLimit);
            valueDescription = valueDescription.replace("#reward#", maxAmount + " " + currency);
        } else {
            valueDescription = amount + " " + currency;
        }
        return valueDescription;
    }

    public String valueOrMaxValue() {
        if(maxAmount > 0) {
            return Integer.toString(maxAmount);
        } else {
            return Integer.toString(amount);
        }
    }

    public static Iterable<Reward> summarize(Iterable<Reward> rewards) {
        Map<String, ArrayList<Reward>> rewardsByCurrency = new HashMap<>();
        for (Reward reward : rewards) {
            if(rewardsByCurrency.containsKey(reward.currency)) {
                ArrayList<Reward> rewardsListByCurrency = rewardsByCurrency.get(reward.currency);
                rewardsListByCurrency.add(reward);
            } else {
                ArrayList<Reward> rewardsListByCurrency = new ArrayList<>();
                rewardsListByCurrency.add(reward);
                rewardsByCurrency.put(reward.currency, rewardsListByCurrency);
            }
        }

        List<Reward> summarizedList = new ArrayList<>();

        for (Map.Entry<String, ArrayList<Reward>> entry : rewardsByCurrency.entrySet()) {
            int amount = 0;
            int maxAmount = 0;
            String currency = entry.getKey();
            for (Reward reward :
                    entry.getValue()) {
                amount += reward.amount;
                maxAmount += reward.maxAmount;
            }
            summarizedList.add(new Reward(amount, maxAmount, currency, Type.UNKNOWN));
        }

        return summarizedList;
    }


    public static String incentiveTextForRewards(Context context, Iterable<Reward> rewards) {
        if(rewards == null ) {
            return null;
        }

        long currencyCount = Observable.fromIterable(rewards)
                .map(reward -> reward.currency)
                .distinct()
                .count()
                .blockingGet();

        if (currencyCount == 1) {
            return handleOneTotalCurrency(context, rewards);
        }

        String potentialTotalRewards = enumerateAsText(context, Observable.fromIterable(rewards)
                .map(reward -> reward.valueDescription(context))
                .toList()
                .blockingGet());

        return context.getString(R.string.reward_incenitiveText).replace("#potentialTotalReward#", potentialTotalRewards);
    }


    private static String handleOneTotalCurrency(Context context, Iterable<Reward> rewards) {
        int totalAmount = 0;
        for (Reward reward: rewards) {
            totalAmount += reward.amount;
        }

        int maxTotalAmount = 0;
        for (Reward reward: rewards) {
            maxTotalAmount += (reward.maxAmount == 0 ? reward.amount : reward.amount);
        }

        String currency = rewards.iterator().next().currency;
        String currencySuffix = currency == null ? "" : currency;

        boolean hasPotentiallyHigherAmount = maxTotalAmount > totalAmount;
        long rewardsTypeCount = Observable.fromIterable(rewards)
                .map(reward -> reward.type)
                .distinct()
                .count()
                .blockingGet();
        boolean useRewardWithLimit = hasPotentiallyHigherAmount || rewardsTypeCount > 1;

        String potentialTotalReward;

        if (useRewardWithLimit) {
            potentialTotalReward = context.getString(R.string.reward_withLimit)
                    .replace("#reward#", maxTotalAmount + " " + currencySuffix);
        } else {
            potentialTotalReward = totalAmount + " " + currencySuffix;
        }

        return context.getString(R.string.reward_incenitiveText).replace("#potentialTotalReward#", potentialTotalReward);
    }


    private static String enumerateAsText(Context context, List<String> rewards) {
        switch (rewards.size()) {
            case 1 : return rewards.get(0);
            case 2 : return context.getString(R.string.enum_two)
                    .replace("#first#", rewards.get(0))
                    .replace("#second#", rewards.get(1));
            default:
                return context.getString(R.string.enum_moreThanTwo)
                        .replace("#commaSeparated#", TextUtils.join(", ", rewards));
        }
    }

    public static String enumerateRewardsAsText(Context context, Iterable<Reward> rewards) {
        List<String> summarizedStrings = Observable.fromIterable(summarize(rewards))
                .map(reward -> reward.valueDescription(context))
                .toList()
                .blockingGet();

        return enumerateAsText(context, summarizedStrings);
    }

    public static String makeConfirmationTextForRewards(Context context, Iterable<Reward> rewards) {
        Iterable<Reward> summarizedRewards = summarize(rewards);
        List<String> rewardsList = new ArrayList<>();
        for (Reward reward:
             summarizedRewards) {
            rewardsList.add(reward.amount + " " + reward.currency);
        }

        return context.getString(R.string.transaction_confirmationText)
                .replace("#oneOrMoreRewards#", enumerateAsText(context, rewardsList));
    }


}
