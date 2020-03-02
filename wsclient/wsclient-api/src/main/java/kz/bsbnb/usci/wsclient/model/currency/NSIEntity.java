package kz.bsbnb.usci.wsclient.model.currency;

import kz.bsbnb.usci.model.persistence.Persistable;

public class NSIEntity extends Persistable {
    private NSIEntitySystem NSIEntitySystem;
    private Object NSIEntityCustom;

    public NSIEntitySystem getNSIEntitySystem() {
        return NSIEntitySystem;
    }

    public void setNSIEntitySystem(NSIEntitySystem NSIEntitySystem) {
        this.NSIEntitySystem = NSIEntitySystem;
    }

    public Object getNSIEntityCustom() {
        return NSIEntityCustom;
    }

    public void setNSIEntityCustom(Object NSIEntityCustom) {
        this.NSIEntityCustom = NSIEntityCustom;
    }
}
