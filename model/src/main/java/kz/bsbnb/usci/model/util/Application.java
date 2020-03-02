package kz.bsbnb.usci.model.util;

import kz.bsbnb.usci.model.persistence.Persistable;

/**
 * @author Jandos Iskakov
 * */

public class Application extends Persistable {
    private String module;
    private String host;
    private Integer port;
    private String product;
    private Integer rmiPort;

    public Application() {
        super();
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getRmiPort() {
        return rmiPort;
    }

    public void setRmiPort(Integer rmiPort) {
        this.rmiPort = rmiPort;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    @Override
    public String toString() {
        return "Application{" +
                "module='" + module + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", product='" + product + '\'' +
                ", rmiPort=" + rmiPort +
                '}';
    }

}
