package com.ykbjson.lib.screening;

import com.ykbjson.lib.screening.xml.DLNAUDA10ServiceDescriptorBinderSAXImpl;

import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.binding.xml.ServiceDescriptorBinder;


/**
 * Description：DLNABrowserService
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-10
 */
public class DLNABrowserService extends AndroidUpnpServiceImpl {
    @Override
    protected UpnpServiceConfiguration createConfiguration() {
        return new AndroidUpnpServiceConfiguration() {
            @Override
            public ServiceDescriptorBinder createServiceDescriptorBinderUDA10() {
                return new DLNAUDA10ServiceDescriptorBinderSAXImpl();
            }
        };
    }
}
