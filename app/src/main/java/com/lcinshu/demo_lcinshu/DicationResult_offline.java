package com.lcinshu.demo_lcinshu;

import java.util.List;

/**
 * Created by lcinshu on 16-12-14.
 * 未使用，因本地识别和云端识别返回json格式数据不同，尚待解析。
 */

public class DicationResult_offline {
    private String sn;
    private String ls;
    private String bg;
    private String ed;

    private List<Words> ws;


    public static class Words {
        private String bg;
        private List<Cw> cw;
        private String slot;

        public String getBg() {
            return bg;
        }

        public void setBg(String bg) {
            this.bg = bg;
        }

        public static class Cw {
            private String w;
            private String sc;

            public String getW() {
                return w;
            }

            public void setW(String w) {
                this.w = w;
            }

            public String getSc() {
                return sc;
            }

            public void setSc(String sc) {
                this.sc = sc;
            }

            @Override
            public String toString() {
                return w;
            }
        }

        public List<Cw> getCw() {
            return cw;
        }

        public void setCw(List<Cw> cw) {
            this.cw = cw;
        }

        public String toString() {
            String result = "";
            for (Cw cwTmp : cw) {
                result += cwTmp.toString();
            }
            return result;
        }

        public String getSlot() {
            return slot;
        }

        public void setSlot(String slot){
            this.slot = slot;
        }
    }


    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getLs() {
        return ls;
    }

    public void setLs(String ls) {
        this.ls = ls;
    }

    public String getBg() {
        return bg;
    }

    public void setBg(String bg) {
        this.bg = bg;
    }

    public String getEd() {
        return ed;
    }

    public void setEd(String ed) {
        this.ed = ed;
    }


    private String sc;
    public String getSc(){
        return sc;
    }

    public void setSc(String sc){
        this.sc = sc;
    }

    public List<Words> getWs() {
        return ws;
    }


    public void setWs(List<Words> ws) {
        this.ws = ws;
    }


    @Override
    public String toString() {
        String result = "";
        for (Words wsTmp : ws) {
            result += wsTmp.toString();
        }
        return result;
    }
}
