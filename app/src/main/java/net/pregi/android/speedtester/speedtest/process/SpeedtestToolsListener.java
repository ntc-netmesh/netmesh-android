package net.pregi.android.speedtester.speedtest.process;

import net.pregi.networking.speedtest.OnSpeedtestListener;
import net.pregi.networking.speedtest.ServerEntry;

import java.util.List;

public interface SpeedtestToolsListener extends OnSpeedtestListener {
    /** <p>Called when a server list is downloaded.
     * onChangeSelectedServer() is called afterwards.</p>
     */
    public void onDownloadServerList(List<ServerEntry> list, Exception e);
    /** <p>Called when a change on selected server occurs.
     * Also called when first adding the listener.</p>
     */
    public void onChangeSelectedServer(ServerEntry value);
    /** <p>Called when logs were updated. A full log is given.</p>
     *
     * @param fullLog the full log.
     */
    public void onUpdateLog(CharSequence fullLog);
}
