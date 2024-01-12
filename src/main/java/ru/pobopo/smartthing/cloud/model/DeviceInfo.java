package ru.pobopo.smartthing.cloud.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class DeviceInfo {
    private String ip;
    private String type;
    private String name;

    public DeviceInfo(String ip, String name) {
        this.ip = ip;
        this.name = name;
        this.type = "type_missing";
    }

    public DeviceInfo(String ip, String type, String name) {
        this.ip = ip;
        this.type = type;
        this.name = name;
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(ip) || StringUtils.isEmpty(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        DeviceInfo comp = (DeviceInfo) obj;
        return StringUtils.equals(comp.getIp(), getIp())
               && StringUtils.equals(comp.getName(), getName());
    }

    @Override
    public int hashCode() {
        int hashCode = ip.hashCode();
        hashCode = 31 * hashCode + name.hashCode();
        return hashCode;
    }
}
