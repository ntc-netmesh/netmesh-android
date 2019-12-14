package net.pregi.android.speedtester.speedtest.process;

import android.Manifest;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;

import com.pregi.android.speedtester.R;
import net.pregi.android.telephony.CellInfoUtils;
import net.pregi.android.text.SpanUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamineNetworksProcessB {
    public interface OnLogListener {
        public void onLog(CharSequence log);
    }

    public static final String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
    };

    public static final int PROP_OUTPUTLOG = 2;

    private static final int COLOR_MINAPI18 = 0xFF444444;
    private static final int COLOR_MINAPI22 = 0xFF006600;
    private static final int COLOR_MINAPI28 = 0xFF000066;
    private static final int COLOR_NULL = 0xFF888888;
    private static final int COLOR_ERROR = Color.RED;

    /** <p>Print all network information accessible to the app.
     * Available information depends on the phone's API level and permissions given by the user.</p>
     *
     * @param context
     * @param onStateChangeListener
     */
    public static void examineNetworks(final Context context, OnLogListener onLogListener) {
        final Resources r = context.getResources();

        onLogListener.onLog(r.getText(R.string.speedtest_html_examinenetworks_loading));

        SpannableStringBuilder output =  new SpannableStringBuilder(r.getText(R.string.speedtest_html_examinenetworks_main));

        CharSequence messageUnavailable = r.getText(R.string.message_unavailable);
        CharSequence messagePermissionDenied = r.getText(R.string.message_permissiondenied);
        CharSequence valueYesR = r.getText(R.string.value_yes);
        CharSequence valueNoR = r.getText(R.string.value_no);
        CellInfoUtils cellInfoUtils = new CellInfoUtils().setMessageUnavailable(messageUnavailable);

        int apiLevel = Build.VERSION.SDK_INT;
        NetworkProperties np = NetworkProperties.getProperties(context);

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        {
            // Get the values we want to insert in the text.
            Map<String, CharSequence> valueMap = new HashMap<String, CharSequence>();

            // API level
            valueMap.put("myApiLevel", apiLevel + " (" + Build.VERSION.RELEASE + ")");

            // network name
            valueMap.put("networkOperatorName", np.getNetworkOperatorName());
            valueMap.put("simOperatorName", np.getSimOperatorName());
            valueMap.put("networkType", cellInfoUtils.toNetworkTypeString(np.getNetworkType()));

            // current active connection
            valueMap.put("networkTypeName", np.getActiveNetworkTypeName());
            valueMap.put("networkSubtypeName", np.getActiveNetworkSubtypeName());
            valueMap.put("isConnected", np.getIsConnected() ? valueYesR : valueNoR );
            valueMap.put("failureReason", np.getFailureReason());
            valueMap.put("extraInfo", np.getExtraInfo());

            applyAnnotations(output, valueMap);
        }

        if (apiLevel >= Build.VERSION_CODES.LOLLIPOP_MR1){
            // Subscription. API 22.
            try {
                SpannableStringBuilder text = new SpannableStringBuilder(r.getText(R.string.speedtest_html_examinenetworks_subscriptioninfo));
                CharSequence itemTextR = r.getText(R.string.speedtest_html_examinenetworks_subscriptioninfo_item);

                SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                // documentation indicates this and the list's size might not be the same.
                // after testing, activeSubscriptionInfoCount seems to return less than list size
                //      if a SIM is disabled.
                int activeSubscriptionInfoCount = sm.getActiveSubscriptionInfoCount();
                //List<SubscriptionInfo> activeSubscriptionInfos = sm.getActiveSubscriptionInfoList();

                Map<String, CharSequence> valueMap = new HashMap<String, CharSequence>();
                valueMap.put("activeSubscriptionInfoCount", Integer.toString(activeSubscriptionInfoCount));
                //valueMap.put("activeSubscriptionInfoListSize", Integer.toString(activeSubscriptionInfos.size()));

                int validSimCount = 0;
                for (int i=0; i<10; i++) {
                    SubscriptionInfo si = sm.getActiveSubscriptionInfoForSimSlotIndex(i);
                    if (si == null) {
                        continue;
                    }
                    validSimCount++;

                    SpannableStringBuilder itemText = new SpannableStringBuilder(new SpannableString(itemTextR));

                    Map<String, CharSequence> itemMap = new HashMap<String, CharSequence>();
                    Map<String, Drawable> drawableMap = new HashMap<String, Drawable>();

                    // Get icon of the carrier.
                    drawableMap.put("icon", new BitmapDrawable(r, si.createIconBitmap(context)));

                    // Get details of the carrier.
                    itemMap.put("id", Integer.toString(si.getSubscriptionId()));
                    // itemMap.put("iccId", si.getIccId());
                    itemMap.put("carrierName", si.getCarrierName());
                    itemMap.put("displayName", si.getDisplayName());
                    itemMap.put("slotIndex", Integer.toString(si.getSimSlotIndex()));
                    itemMap.put("subId", Integer.toString(si.getSubscriptionId()));

                    int mcc = si.getMcc();
                    int mnc = si.getMnc();
                    itemMap.put("mccMnc", cellInfoUtils.toMobileNetworkCodeString(mcc, mnc));

                    applyAnnotations(itemText, itemMap, drawableMap);
                    text.append(itemText);
                }

                if (validSimCount>=2 && apiLevel == Build.VERSION_CODES.LOLLIPOP_MR1) {
                    text.append(SpanUtils.colored(r.getText(R.string.speedtest_html_examinenetworks_subscriptioninfo_warning_multisim), COLOR_ERROR));
                }

                applyAnnotations(text, valueMap);
                output.append(text);
            } catch (SecurityException e) {
                // Happens when READ_PHONE_STATE is not granted.
                SpannableStringBuilder text = new SpannableStringBuilder(r.getText(R.string.speedtest_html_examinenetworks_subscriptioninfo_unavailable_permission));
                output.append(applyAnnotations(text, null));
            }
        } else {
            SpannableStringBuilder text = new SpannableStringBuilder(r.getText(R.string.speedtest_html_examinenetworks_subscriptioninfo_unavailable_minApi));
            output.append(applyAnnotations(text, null));
        }

        // cellinfo
        {
            SpannableStringBuilder text = new SpannableStringBuilder(r.getText(R.string.speedtest_html_examinenetworks_cellinfo_main));

            Map<String, CharSequence> valueMap = new HashMap<String, CharSequence>();

            try {
                List<CellInfo> cellInfos = tm.getAllCellInfo();

                if (cellInfos != null) {
                    CharSequence cellInfoItemR = r.getText(R.string.speedtest_html_examinenetworks_cellinfo_item_main);
                    CharSequence messageApi28 = r.getText(R.string.message_minApi_28);
                    CharSequence itemCdma = null;
                    CharSequence itemGsm = null;
                    CharSequence itemLte = null;
                    CharSequence itemWcdma = null;

                    valueMap.put("cellInfoCount", Integer.toString(cellInfos.size()));

                    for (CellInfo ci : cellInfos) {
                        SpannableStringBuilder itemText = new SpannableStringBuilder(cellInfoItemR);

                        Map<String, CharSequence> itemValueMap = new HashMap<String, CharSequence>();

                        itemValueMap.put("isRegistered", ci.isRegistered() ? valueYesR : valueNoR);
                        if (apiLevel >= Build.VERSION_CODES.P) {
                            String value = "unknown";
                            switch(ci.getCellConnectionStatus()) {
                                case CellInfo.CONNECTION_NONE:
                                    value = "not serving";
                                    break;
                                case CellInfo.CONNECTION_PRIMARY_SERVING:
                                    value = "primary serving";
                                    break;
                                case CellInfo.CONNECTION_SECONDARY_SERVING:
                                    value = "secondary serving";
                                    break;
                            }
                            itemValueMap.put("cellConnectionStatus", value);
                        } else {
                            itemValueMap.put("cellConnectionStatus", messageApi28);
                        }

                        if (ci instanceof CellInfoCdma) {
                            itemValueMap.put("cellType", "CDMA");
                            itemText.append(itemCdma != null ? itemCdma : (itemCdma=r.getText(R.string.speedtest_html_examinenetworks_cellinfo_item_cdma)));

                            CellInfoCdma cdma = (CellInfoCdma)ci;
                            CellIdentityCdma cdmaId = cdma.getCellIdentity();
                            CellSignalStrengthCdma cdmaSignal = cdma.getCellSignalStrength();

                            // Cell Identity
                            itemValueMap.put("baseStationId", cellInfoUtils.toBaseStationString(cdmaId.getBasestationId()));
                            itemValueMap.put("geolocation", cellInfoUtils.toGeolocationString(cdmaId.getLatitude(), cdmaId.getLongitude()));
                            itemValueMap.put("networkId", cellInfoUtils.toNetworkIdString(cdmaId.getNetworkId()));
                            itemValueMap.put("systemId", cellInfoUtils.toSystemIdString(cdmaId.getSystemId()));

                            if (apiLevel >= Build.VERSION_CODES.P) {
                                itemValueMap.put("operatorName", cdmaId.getOperatorAlphaLong());
                            } else {
                                itemValueMap.put("operatorName", messageApi28);
                            }

                            // Cell Signal Strength
                            itemValueMap.put("signalStrength", Integer.toString(cdmaSignal.getCdmaDbm()));
                            itemValueMap.put("signalToNoise", Integer.toString(cdmaSignal.getEvdoSnr()));
                            itemValueMap.put("signalQuality", Integer.toString(cdmaSignal.getLevel()));
                        } else if (ci instanceof CellInfoGsm) {
                            itemValueMap.put("cellType", "GSM");
                            itemText.append(itemGsm != null ? itemGsm : (itemGsm=r.getText(R.string.speedtest_html_examinenetworks_cellinfo_item_gsm)));

                            CellInfoGsm gsm = (CellInfoGsm)ci;
                            CellIdentityGsm gsmId = gsm.getCellIdentity();
                            CellSignalStrengthGsm gsmSignal = gsm.getCellSignalStrength();

                            itemValueMap.put("cellId", cellInfoUtils.toCellIdString(gsmId.getCid()));
                            itemValueMap.put("locationAreaCode", cellInfoUtils.toLocationAreaCodeString(gsmId.getLac()));
                            itemValueMap.put("mccMnc", cellInfoUtils.toMobileNetworkCodeString(gsmId.getMcc(), gsmId.getMnc()));
                            itemValueMap.put("signalStrength", Integer.toString(gsmSignal.getDbm()));
                            itemValueMap.put("signalQuality", Integer.toString(gsmSignal.getLevel()));

                            if (apiLevel >= Build.VERSION_CODES.P) {
                                itemValueMap.put("operatorName", gsmId.getOperatorAlphaLong());
                            } else {
                                itemValueMap.put("operatorName", messageApi28);
                            }
                        } else if (ci instanceof CellInfoLte) {
                            itemValueMap.put("cellType", "LTE");
                            itemText.append(itemLte != null ? itemLte : (itemLte=r.getText(R.string.speedtest_html_examinenetworks_cellinfo_item_lte)));

                            CellInfoLte lte = (CellInfoLte)ci;
                            CellIdentityLte lteId = lte.getCellIdentity();
                            CellSignalStrengthLte lteSignal = lte.getCellSignalStrength();

                            itemValueMap.put("cellId", cellInfoUtils.toIntOrUnavailable(lteId.getCi()));
                            itemValueMap.put("mccMnc", cellInfoUtils.toMobileNetworkCodeString(lteId.getMcc(), lteId.getMnc()));
                            itemValueMap.put("trackingAreaCode", cellInfoUtils.toIntOrUnavailable(lteId.getTac()));
                            itemValueMap.put("timingAdvance", cellInfoUtils.toIntOrUnavailable(lteSignal.getTimingAdvance()));
                            itemValueMap.put("signalStrength", Integer.toString(lteSignal.getDbm()));
                            itemValueMap.put("signalQuality", Integer.toString(lteSignal.getLevel()));

                            if (apiLevel >= Build.VERSION_CODES.P) {
                                itemValueMap.put("operatorName", lteId.getOperatorAlphaLong());
                                itemValueMap.put("bandwidth", cellInfoUtils.toIntOrUnavailable(lteId.getBandwidth()));
                            } else {
                                itemValueMap.put("operatorName", messageApi28);
                                itemValueMap.put("bandwidth", messageApi28);
                            }
                        } else {
                            itemValueMap.put("cellType", "Unknown");
                            if (apiLevel >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                if (ci instanceof CellInfoWcdma) {
                                    itemValueMap.put("cellType", "WCDMA");
                                    itemText.append(itemWcdma != null ? itemWcdma : (itemWcdma=r.getText(R.string.speedtest_html_examinenetworks_cellinfo_item_wcdma)));

                                    CellInfoWcdma wcdma = (CellInfoWcdma)ci;
                                    CellIdentityWcdma wcdmaId = wcdma.getCellIdentity();
                                    CellSignalStrengthWcdma wcdmaSignal = wcdma.getCellSignalStrength();

                                    itemValueMap.put("cellId", cellInfoUtils.toIntOrUnavailable(wcdmaId.getCid()));
                                    itemValueMap.put("locationAreaCode", cellInfoUtils.toLocationAreaCodeString(wcdmaId.getLac()));
                                    itemValueMap.put("mccMnc", cellInfoUtils.toMobileNetworkCodeString(wcdmaId.getMcc(), wcdmaId.getMnc()));
                                    itemValueMap.put("signalStrength", Integer.toString(wcdmaSignal.getDbm()));
                                    itemValueMap.put("signalQuality", Integer.toString(wcdmaSignal.getLevel()));

                                    if (apiLevel >= Build.VERSION_CODES.P) {
                                        itemValueMap.put("operatorName", wcdmaId.getOperatorAlphaLong());
                                    } else {
                                        itemValueMap.put("operatorName", messageApi28);
                                    }
                                }
                            }
                        }

                        applyAnnotations(itemText, itemValueMap);
                        text.append(itemText);
                        text.append("\n");
                    }
                } else {
                    valueMap.put("cellInfoCount", messageUnavailable);
                }
            } catch (SecurityException e) {
                // Happens when ACCESS_FINE_LOCATION is not granted.
                valueMap.put("cellInfoCount", SpanUtils.colored(messagePermissionDenied, COLOR_ERROR));
            }

            applyAnnotations(text, valueMap);
            output.append(text);
        }

        // neighboring cell info
        if (apiLevel <= Build.VERSION_CODES.P) {
            CharSequence messageUnknown = r.getText(R.string.message_unknown);

            SpannableStringBuilder text = new SpannableStringBuilder(r.getText(R.string.speedtest_html_examinenetworks_ncellinfo_main));

            Map<String, CharSequence> valueMap = new HashMap<String, CharSequence>();

            try {
                List<NeighboringCellInfo> nCellInfos = tm.getNeighboringCellInfo();

                if (nCellInfos != null) {
                    CharSequence nCellInfoItemR = r.getText(R.string.speedtest_html_examinenetworks_ncellinfo_item_main);

                    valueMap.put("cellInfoCount", Integer.toString(nCellInfos.size()));

                    for (NeighboringCellInfo nci : nCellInfos) {
                        SpannableStringBuilder itemText = new SpannableStringBuilder(nCellInfoItemR);

                        Map<String, CharSequence> itemValueMap = new HashMap<String, CharSequence>();

                        itemValueMap.put("cellId", nci.getCid() != -1 ? Integer.toString(nci.getCid()) : messageUnknown);
                        itemValueMap.put("location", nci.getLac() != -1 ? Integer.toString(nci.getLac()) : messageUnknown);
                        {
                            CharSequence value = cellInfoUtils.toNetworkTypeString(nci.getNetworkType());
                            if (value == null) {
                                value = messageUnknown;
                            }
                            itemValueMap.put("networkType", value);
                        }
                        {
                            CharSequence value = cellInfoUtils.rssiToDbmString(nci.getNetworkType(), nci.getRssi());
                            if (value == null) {
                                value = messageUnknown;
                            }
                            itemValueMap.put("signalStrength", value);
                        }

                        applyAnnotations(itemText, itemValueMap);
                        text.append(itemText);
                        text.append("\n");
                    }
                } else {
                    valueMap.put("cellInfoCount", messageUnavailable);
                }
            } catch (SecurityException e) {
                // Happens when ACCESS_COARSE_LOCATION is not granted.
                valueMap.put("cellInfoCount", SpanUtils.colored(messagePermissionDenied, COLOR_ERROR));
            }

            applyAnnotations(text, valueMap);
            output.append(text);
        }

        onLogListener.onLog(output);
    }


    private static SpannableStringBuilder applyAnnotations(SpannableStringBuilder builder, Map<String, CharSequence> valueMap) {
        return applyAnnotations(builder, valueMap, null);
    }

    private static SpannableStringBuilder applyAnnotations(SpannableStringBuilder builder, Map<String, CharSequence> valueMap, Map<String, Drawable> drawableMap) {
        Annotation[] annotations = builder.getSpans(0, builder.length(), Annotation.class);
        for (Annotation a : annotations) {
            int start =  builder.getSpanStart(a);
            int end = builder.getSpanEnd(a);
            String aKey = a.getKey();
            String aValue = a.getValue();

            if ("minApi".equals(aKey)) {
                // apply styling to annotations indicating text with relevant minimum API.

                if ("22".equals(aValue)) {
                    builder.setSpan(new ForegroundColorSpan(COLOR_MINAPI22), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                } else if ("28".equals(aValue)) {
                    builder.setSpan(new ForegroundColorSpan(COLOR_MINAPI28), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                } else if ("18".equals(aValue)) {
                    builder.setSpan(new ForegroundColorSpan(COLOR_MINAPI18), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
            } else if ("value".equals(aKey)) {
                if (valueMap != null) {
                    // insert the value into where the annotation is found.
                    CharSequence value = valueMap.get(aValue);

                    if (value != null) {
                        if (value.length() > 0) {
                            builder.replace(start, end, value);
                        } else {
                            builder.replace(start, end, "<blank>");
                            builder.setSpan(new ForegroundColorSpan(COLOR_NULL), start, start + 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    } else {
                        builder.replace(start, end, "null");
                        builder.setSpan(new ForegroundColorSpan(COLOR_NULL), start, start + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            } else if ("image".equals(aKey)) {
                if (drawableMap != null) {
                    Drawable value = drawableMap.get(aValue);

                    if (value != null) {
                        builder.setSpan(new ImageSpan(value), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            } else if ("tag".equals(aKey)) {
                // Apparently, at least for API 22 where this code is tested, when reusing styled strings
                //       and ultimately appending each reuse in a single SpannableStringBuilder,
                //      the style tags are lost after using it once
                // It doesn't matter if there are a number of new SpannableString(...) or new SpannableStringBuilder(...) in between
                //      this reused resource and the final output SpannableStringBuilder
                //      or if the resource is re-retrieved with Resources#getText(...).
                // At least the annotation spans appear intact.
                String tagName = aValue;
                if ("b".equals(tagName)) {
                    builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                } else if ("i".equals(tagName)) {
                    builder.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }
            // Remove the span so it is only applied once.
            builder.removeSpan(a);
        }
        return builder;
    }
}
