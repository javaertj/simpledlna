package com.ykbjson.app.simpledlna;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ykbjson.lib.screening.bean.DeviceInfo;

import org.fourthline.cling.model.meta.Device;

/**
 * Description：设备列表适配器
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-18
 */

public class DevicesAdapter extends ArrayAdapter<DeviceInfo> {
    private LayoutInflater mInflater;

    public DevicesAdapter(Context context) {
        super(context, 0);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = mInflater.inflate(R.layout.item_device, parent, false);

        DeviceInfo item = getItem(position);
        if (item == null) {
            return convertView;
        }

        Device device = item.getDevice();

        ImageView imageView = convertView.findViewById(R.id.listview_item_image);
        imageView.setBackgroundResource(R.drawable.ic_action_dock);

        TextView textView = convertView.findViewById(R.id.listview_item_line_one);
        textView.setText(device.getDetails().getFriendlyName());

        return convertView;
    }
}