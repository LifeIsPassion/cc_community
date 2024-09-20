package com.cyt.community.entity;

public class Page {
    //当前页
    private int current = 1;
    //页显示的帖子上限
    private int limit = 10;
    //帖子的总数
    private int rows;
    //帖子的路径
    private String path;


    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        if(current >= 1)
        this.current = current;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        if(limit >= 1 && limit <= 100)
        this.limit = limit;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if(rows > 0)
        this.rows = rows;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * 获取当前页的起始行
     *
     * @return
     */
    public int getOffset(){
        return (current - 1) * limit;
    }
    /**
     * 获取总页数
     *
     * @return
     */
    public int getPageCounts(){
        if(rows % limit == 0){
            return rows / limit;
        }else{
            return rows / limit + 1;
        }
    }
    /**
     * 获取起始页
     *
     * @return
     */
    public int getStart(){
        int start = current - 2;
        return start > 1 ? start:1;
    }

    /**
     * 获取最终页
     *
     * @return
     */
    public int getEnd(){
        int end = current + 2;
        return end > getPageCounts() ? getPageCounts():end;
    }
}
