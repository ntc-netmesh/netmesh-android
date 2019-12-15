package net.pregi.android.netmesh.speedtest.ui.main;

import android.app.Activity;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import net.pregi.android.netmesh.R;
import net.pregi.android.text.SpanUtils;
import net.pregi.networking.speedtest.ServerEntry;

import java.util.ArrayList;
import java.util.List;

public class ServerListAdapter extends BaseAdapter {
    private Context context;
    private List<ServerEntry> serverList = new ArrayList<>();

    public ServerListAdapter(Fragment fragment) {
        this.context = fragment.getContext();
    }
    public ServerListAdapter(Activity activity) {
        this.context = activity;
    }

    public void setServerList(List<ServerEntry> list) {
        if (list != null) {
            serverList = list;
        } else {
            serverList = new ArrayList<>();
        }
        if (context instanceof Activity) {
            ((Activity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        } else {
            notifyDataSetChanged();
        }
    }
    public List<ServerEntry> getServerList() {
        return serverList;
    }

    @Override
    public int getCount() {
        return serverList.size();
    }

    @Override
    public ServerEntry getItem(int position) {
        return serverList.size() > 0 ? serverList.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        ServerEntry entry = getItem(position);
        return entry != null ? entry.getId() : -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.adapter_speedtest_serverlist_item, null);
        }

        ServerEntry entry = getItem(position);

        // There seems to be an issue with the spinner's dropdown and the item's TextViews with match_parent
        // If the text would be long enough to be longer than the TextView,
        //      getView will be repeatedly called while the dropdown is visible,
        // If you log parent.getMeasuredWidth() here,
        //      you will get two alternating values continuously being logged.
        // This happens even if ellipsize and singleLine is enabled.
        // It wasn't enough to avoid using wrap_content, it seems.

        // This effect can be hackishly stifled by manually setting the layout width
        //      width a little gap so the measuring mechanism won't be inadvertently called again.

        // Whether this only happens with a specific device is not clear.

        int matchParentWidth = parent.getMeasuredWidth()-30;
        if (matchParentWidth>0) {
            {
                TextView nameAndSponsorText = convertView.findViewById(R.id.serverentry_name_and_sponsor);
                if (nameAndSponsorText != null) {
                    ViewGroup.LayoutParams layoutParams = nameAndSponsorText.getLayoutParams();
                    layoutParams.width = matchParentWidth;

                    SpannableStringBuilder text = new SpannableStringBuilder();
                    if (entry != null) {
                        text.append(SpanUtils.bold(entry.getName()));
                        text.append(", ");
                        text.append(new SpanUtils.Builder(entry.getSponsor() + ", " + entry.getCountryCode()).add(new RelativeSizeSpan(0.7f)).build());
                    } else {
                        text.append("");
                    }

                    // setSelected(true) enables marquee, if set in xml
                    // I'm hesitant, but long texts will remain cut off otherwise.
                    nameAndSponsorText.setSelected(true);

                    nameAndSponsorText.setText(text);
                }
            }
            {
                TextView nameText = convertView.findViewById(R.id.serverentry_name);
                if (nameText != null) {
                    ViewGroup.LayoutParams layoutParams = nameText.getLayoutParams();
                    layoutParams.width = matchParentWidth;

                    nameText.setSelected(true);

                    nameText.setText(entry.getName());
                }
            }
            {
                TextView sponsorText = convertView.findViewById(R.id.serverentry_sponsor);
                if (sponsorText != null) {
                    ViewGroup.LayoutParams layoutParams = sponsorText.getLayoutParams();
                    layoutParams.width = matchParentWidth;

                    sponsorText.setSelected(true);

                    sponsorText.setText(entry.getSponsor());
                }
            }

            {
                TextView hostText = convertView.findViewById(R.id.serverentry_host);
                ViewGroup.LayoutParams layoutParams = hostText.getLayoutParams();
                layoutParams.width = matchParentWidth;
                hostText.setText(entry != null ? entry.getHost() + (entry.getPort()>=0 ? ":" + entry.getPort() : "") : "");
            }
        }

        //Log.d("OptionsFragment", "GetView, position: "+position+" mw: "+parent.getMeasuredWidth());

        return convertView;
    }
}
