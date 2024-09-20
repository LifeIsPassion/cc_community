package com.cyt.community.util;

import org.apache.commons.lang3.CharUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    private final String REPLACEMENT = "***";

    private TrieNode rootNode = new TrieNode();

    @PostConstruct
    public void init(){
        try(
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        ){
            String keyWord;
            while ((keyWord = bufferedReader.readLine()) != null){
                this.addKeyWord(keyWord);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //往过滤树中添加敏感词
    public void addKeyWord(String keyWord){
        TrieNode tmpNode = rootNode;
        for(int i = 0;i < keyWord.length();i++){
            char c = keyWord.charAt(i);
            TrieNode newNode = tmpNode.getNode(c);
            if (newNode == null){
                newNode = new TrieNode();
                tmpNode.addNode(c,newNode);
            }
            tmpNode = newNode;

            if(i == keyWord.length() - 1){
                tmpNode.setIsKeyEnd(true);
            }
        }
    }

    //过滤敏感词
    public String filter(String text){
        //过滤树指针
        TrieNode tmpNode = rootNode;
        //文本指针
        int posistion = 0;
        //敏感词指针
        int begin = 0;
        //
        StringBuffer stringBuffer = new StringBuffer();
        while (posistion < text.length()){
            Character c = text.charAt(posistion);
            //判断字符是否合法
            if(isAllow(c)){
                //非法，但是没有前缀，孤立字符
                if(tmpNode == rootNode){
                    stringBuffer.append(c);
                    begin++;
                }
                posistion++;
                continue;
            }
            tmpNode = tmpNode.getNode(c);
            if(tmpNode == null){
                stringBuffer.append(c);
                posistion = ++begin;
                tmpNode = rootNode;
            }else if(tmpNode.getIsKeyEnd()){
                //到达过滤树的尾部
                stringBuffer.append(REPLACEMENT);
                begin = ++posistion;
                tmpNode = rootNode;
            }else {
                posistion++;
            }
        }
        //如果在文本的最后出现了一个超长的敏感词字符串，但是还没有判定结束的时候就退出循环了，那么此时会出现空字符串留在最后面没有被添加进去
        stringBuffer.append(text.substring(posistion));
        return stringBuffer.toString();
    }

    //判断是否是非法字符
    public boolean isAllow(Character c){
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }

    private class TrieNode{
        private boolean isKeyEnd = false;
        private Map<Character,TrieNode> subNods = new HashMap<>();

        //
        public boolean getIsKeyEnd(){
            return isKeyEnd;
        }
        public void setIsKeyEnd(boolean isKeyEnd){
            this.isKeyEnd = isKeyEnd;
        }
        //添加节点
        public void addNode(Character c,TrieNode trieNode){
            subNods.put(c,trieNode);
        }
        //得到节点
        public TrieNode getNode(Character c){
            return subNods.get(c);
        }
    }

}
