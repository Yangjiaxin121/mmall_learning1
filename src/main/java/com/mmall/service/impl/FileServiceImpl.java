package com.mmall.service.impl;

import com.google.common.collect.Lists;
import com.mmall.service.IFileService;
import com.mmall.util.FTPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service("iFileService")
public class FileServiceImpl implements IFileService {

    Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    public String upLoad(MultipartFile file, String path){

        String fileName = file.getOriginalFilename();

        //获取扩展名
        String fileExtensionName = fileName.substring(fileName.lastIndexOf("."));
        String uploadFileName = UUID.randomUUID().toString()+fileExtensionName;
        logger.info("开始上传文件，上传的文件名{}，上传路径{},新文件名{}",fileName,path,uploadFileName);

        File fileDir = new File(path);
        if (!fileDir.exists()){
            fileDir.mkdirs();
            fileDir.setWritable(true);
        }

        File targetFile = new File(path,uploadFileName);

        try {
            file.transferTo(targetFile);
            //上传文件已经成功
            FTPUtil.uploadFile(Lists.newArrayList(targetFile));
            //todo 将文件上传到ftp服务器

            //todo 删除upload下的文件
            targetFile.delete();

        } catch (IOException e) {
            logger.error("上传文件失败",e);
            return null;
        }

        return targetFile.getName();

    }
}
