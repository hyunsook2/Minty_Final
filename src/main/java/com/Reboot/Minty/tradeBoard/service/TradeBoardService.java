package com.Reboot.Minty.tradeBoard.service;

import com.Reboot.Minty.categories.entity.SubCategory;
import com.Reboot.Minty.member.entity.User;
import com.Reboot.Minty.member.entity.UserLocation;
import com.Reboot.Minty.member.repository.UserLocationRepository;
import com.Reboot.Minty.member.repository.UserRepository;
import com.Reboot.Minty.tradeBoard.dto.TradeBoardDto;
import com.Reboot.Minty.tradeBoard.entity.TradeBoard;
import com.Reboot.Minty.tradeBoard.entity.TradeBoardImg;
import com.Reboot.Minty.tradeBoard.repository.TradeBoardImgRepository;
import com.Reboot.Minty.tradeBoard.repository.TradeBoardRepository;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import jakarta.persistence.EntityNotFoundException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TradeBoardService {
    private final TradeBoardRepository tradeBoardRepository;

    private final TradeBoardImgRepository tradeBoardImgRepository;

    private final UserRepository userRepository;

    private final UserLocationRepository userLocationRepository;
    @Autowired
    private Storage storage;


    public Page<TradeBoard> getAllByBoardType(int boardType, Pageable pageable){
        return tradeBoardRepository.findAllByBoardType(boardType, pageable);
    }

    public Page<TradeBoard> getBoardsByBoardTypeAndSubCategory( int boardType ,Optional<SubCategory> subCategory, Pageable pageable) {
        return tradeBoardRepository.getBoardsByBoardTypeAndSubCategory( boardType, subCategory,pageable);
    }



    @Autowired
    public TradeBoardService(TradeBoardRepository tradeBoardRepository, TradeBoardImgRepository tradeBoardImgRepository, UserRepository userRepository, UserLocationRepository userLocationRepository) {
        this.tradeBoardRepository = tradeBoardRepository;
        this.tradeBoardImgRepository = tradeBoardImgRepository;
        this.userRepository = userRepository;
        this.userLocationRepository = userLocationRepository;
    }
    public TradeBoard save(TradeBoard tradeBoard){
        return tradeBoardRepository.save(tradeBoard);
    }

    public TradeBoard findById(Long boardId){
        return tradeBoardRepository.findById(boardId).orElseThrow(EntityNotFoundException::new);
    }

    public List<TradeBoardImg> getImgList(Long boardId){
        return tradeBoardImgRepository.findByTradeBoardId(boardId);
    }

    @Value("${spring.cloud.gcp.storage.credentials.bucket}")
    private String bucketName;

    public Long saveBoard(Long userId, TradeBoardDto tradeBoardDto, List<MultipartFile> mf) {
        String uuid = UUID.randomUUID().toString();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        UserLocation userLocation = userLocationRepository.findByUserId(user.getId());
        TradeBoard tradeBoard = tradeBoardDto.toEntity(tradeBoardDto);
        tradeBoard.setUser(user);
        tradeBoard.setUserLocation(userLocation);
        MultipartFile firstFile = mf.get(0);
        String thumbnail = uuid;
        tradeBoard.setThumbnail(thumbnail);
        TradeBoard savedTradeBoard= tradeBoardRepository.save(tradeBoard);
        Long targetBoardId = savedTradeBoard.getId();
        try {
            MultipartFile resizedFirstFile = resizeImage(firstFile, 300, 300);

            BlobInfo blobInfo = storage.create(
                    BlobInfo.newBuilder(bucketName, uuid)
                            .setContentType("image/jpg")
                            .build(),
                    resizedFirstFile.getInputStream()
            );
        }catch (IOException e){
            e.printStackTrace();
        }

        try {
            for(int i = 0 ; i<mf.size() ; i++){
                uuid = UUID.randomUUID().toString();
                MultipartFile files = mf.get(i);
                String fileName = uuid;
                MultipartFile resizedFile = resizeImage(files, 640, 640);
                BlobInfo blobInfo = storage.create(
                        BlobInfo.newBuilder(bucketName, uuid)
                                .setContentType("image/jpg")
                                .build(),
                        resizedFile.getInputStream()
                );
                TradeBoardImg tradeBoardImg = new TradeBoardImg();
                tradeBoardImg.setTradeBoard(savedTradeBoard);
                tradeBoardImg.setImgUrl(fileName);
                tradeBoardImgRepository.save(tradeBoardImg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return targetBoardId;
    }



    private MultipartFile resizeImage(MultipartFile file, int width, int height) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        if (originalWidth <= width && originalHeight <= height) {
            return file;
        }
        Thumbnails.Builder<? extends InputStream> thumbnailBuilder = Thumbnails.of(file.getInputStream())
                .size(width, height);

        if (file.getContentType().equals("image/jpeg") || file.getContentType().equals("image/jpg")|| file.getContentType().equals("image/png")|| file.getContentType().equals("image/bmp")) {
            thumbnailBuilder.outputFormat("jpg");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        thumbnailBuilder.toOutputStream(outputStream);

        return new MultipartFile() {
            @Override
            public String getName() {
                return file.getName();
            }

            @Override
            public String getOriginalFilename() {
                return file.getOriginalFilename();
            }

            @Override
            public String getContentType() {
                return file.getContentType();
            }

            @Override
            public boolean isEmpty() {
                return file.isEmpty();
            }

            @Override
            public long getSize() {
                return outputStream.size();
            }

            @Override
            public byte[] getBytes() throws IOException {
                return outputStream.toByteArray();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(outputStream.toByteArray());
            }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                file.transferTo(dest);
            }
        };
    }
}