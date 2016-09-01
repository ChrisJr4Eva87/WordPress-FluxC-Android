package org.wordpress.android.fluxc.utils;

import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;

import java.util.ArrayList;
import java.util.List;

public class SiteUtils {
    public static SiteModel generateDotComSite() {
        SiteModel example = new SiteModel();
        example.setSiteId(556);
        example.setIsWPCom(true);
        example.setIsVisible(true);
        return example;
    }

    public static SiteModel generateSelfHostedNonJPSite() {
        SiteModel example = new SiteModel();
        example.setSelfHostedSiteId(6);
        example.setIsWPCom(false);
        example.setIsJetpack(false);
        example.setIsVisible(true);
        example.setXmlRpcUrl("http://some.url/xmlrpc.php");
        return example;
    }

    public static SiteModel generateJetpackSite() {
        SiteModel example = new SiteModel();
        example.setSiteId(982);
        example.setSelfHostedSiteId(8);
        example.setIsWPCom(false);
        example.setIsJetpack(true);
        example.setIsVisible(true);
        example.setXmlRpcUrl("http://jetpack.url/xmlrpc.php");
        return example;
    }

    public static SiteModel generateJetpackSiteOverRestOnly() {
        SiteModel example = new SiteModel();
        example.setSiteId(5623);
        example.setIsWPCom(false);
        example.setIsJetpack(true);
        example.setIsVisible(true);
        example.setXmlRpcUrl("http://jetpack.url/xmlrpc.php");
        return example;
    }

    public static List<PostFormatModel> generatePostFormats(String... names) {
        List<PostFormatModel> res = new ArrayList<>();
        for (String name : names) {
            PostFormatModel postFormat = new PostFormatModel();
            postFormat.setSlug(name.toLowerCase());
            postFormat.setDisplayName(name);
            res.add(postFormat);
        }
        return res;
    }
}
