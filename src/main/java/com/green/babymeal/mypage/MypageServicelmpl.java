package com.green.babymeal.mypage;

import com.green.babymeal.common.config.properties.AppProperties;
import com.green.babymeal.common.config.redis.RedisService;
import com.green.babymeal.common.config.security.AuthenticationFacade;
import com.green.babymeal.common.entity.*;
import com.green.babymeal.common.repository.*;
import com.green.babymeal.mypage.model.*;
import com.green.babymeal.user.UserRepository;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.misc.Interval;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MypageServicelmpl implements MypageService{

    private final OrderlistRepository orderlistRep;
    private final OrderDetailRepository orderDetailRep;
    private final UserRepository userRep;
    private final PasswordEncoder PW_ENCODER;
    private final AuthenticationFacade USERPK;
    private final ProductCategoryRelationRepository productcaterelationRep;
    private final JPAQueryFactory jpaQueryFactory;
    private final AppProperties appProperties;
    private final RedisService redisService;
    private final BabyAlleRepository babyAlleRep;
    private final BabyRepository babyRep;




    @Override
    public  List<OrderlistSelVo> orderlist (int month){
        QOrderlistEntity orderlist = QOrderlistEntity.orderlistEntity;
        QOrderDetailEntity orderDetail = QOrderDetailEntity.orderDetailEntity;
        QProductEntity product = QProductEntity.productEntity;
        QProductThumbnailEntity thumbnail = QProductThumbnailEntity.productThumbnailEntity;
        UserEntity loginUser = USERPK.getLoginUser();

        //날짜 계산
        LocalDate today = LocalDate.now();
        LocalDate Month = today.minusMonths(month);

        Byte delYn = 0;
        int num = 10;

        List<OrderlistSelVo> order = jpaQueryFactory
                .select(Projections.constructor(OrderlistSelVo.class, orderlist.orderId,orderlist.orderCode,orderlist.createdAt
                        ,orderDetail.totalPrice,orderDetail.productId.pName,thumbnail.img,orderlist.shipment))
                .from(orderlist)
                .leftJoin(orderDetail)
                .on(orderDetail.orderId.orderId.eq(orderlist.orderId))
                .leftJoin(thumbnail)
                .on(orderDetail.productId.productId.eq(thumbnail.productId.productId))
                .where(orderlist.iuser.iuser.eq(loginUser.getIuser()),orderlist.delYn.eq(delYn)
                ,orderlist.createdAt.between(Month, today))
                .groupBy(orderlist.orderId)
                .fetch();

        for (int i = 0; i <order.size(); i++) {
            List<OrderlistDetailVo> orderDetailEntity = orderDetailRep.findByOrderId(order.get(i).getOrderId());
            int totalprice  = 0;

            Long catenum = 0L;
            for (int j = 0; j <orderDetailEntity.size(); j++) {
                int price= orderDetailEntity.get(j).getTotalPrice();
                totalprice += price;

                //카테고리 아이디 찾기
                Long productId = orderDetailEntity.get(j).getProductId();
                ProductEntity productEntity = ProductEntity.builder().productId(productId).build();
                ProductCateRelationEntity cate = productcaterelationRep.findByProductEntity(productEntity);
                Long cateId = cate.getCategoryEntity().getCateId();
                log.info("cateId:{}",cateId);
                catenum = cateId;
            }
            String pName = order.get(i).getPName();
            pName = "["+catenum+"단계] "+ pName;
            order.get(i).setPName(pName);

            order.get(i).setTotalprice(totalprice);
        }

        return order;
    }

    public OrderlistDetailUserVo orderDetail(Long ordercode){
        OrderlistEntity byOrderCode = orderlistRep.findByOrderCode(ordercode);

        List<OrderlistDetailVo> byOrderId = orderDetailRep.findByOrderId(byOrderCode.getOrderId());
        //OrderlistEntity orderlistEntity = orderlistRep.findByOrderId(orderId);
        OrderlistUserVo vo = new OrderlistUserVo();
        vo.setReciever(byOrderCode.getReciever());
        vo.setAddress(byOrderCode.getAddress());
        vo.setAddressDetail(byOrderCode.getAddressDetail());
        vo.setPhoneNm(byOrderCode.getPhoneNm());
        vo.setRequest(byOrderCode.getRequest());
        vo.setUsepoint(byOrderCode.getUsepoint());
        return OrderlistDetailUserVo.builder().list(byOrderId).user(vo).build();

    }
    public OrderlistEntity delorder(Long orderId){
        OrderlistEntity entity = orderlistRep.getReferenceById(orderId);
        Byte delYn = 1;
        //entity.setOrderid(orderId);
        entity.setDelYn(delYn);
        OrderlistEntity save = orderlistRep.save(entity);
        return save;
    }
    public ProfileVo profile(){
        UserEntity loginUser = USERPK.getLoginUser();
        UserEntity userEntity = userRep.findById(loginUser.getIuser()).get();

        List<UserBabyinfoEntity> babyentity = babyRep.findByUserEntity_Iuser(loginUser.getIuser());

        // 아기 정보 받아오기
        List<BabyVo> vo = new ArrayList<>();
        for (int i = 0; i <babyentity.size(); i++) {

            List<UserBabyalleEntity> babyallergyentity = babyAlleRep.findByUserBabyinfoEntity_BabyId(babyentity.get(i).getBabyId());
            List<String>allergyname = new ArrayList<>();
            for (int j = 0; j <babyallergyentity.size(); j++) {
                String allergy = babyallergyentity.get(j).getAllergyEntity().getAllergyName();
                allergyname.add(allergy);
            }
            BabyVo build = BabyVo.builder().babyId(babyentity.get(i).getBabyId())
                    .childBirth(babyentity.get(i).getChildBirth())
                    .allergyname(allergyname).build();
            vo.add(build);
        }

        return ProfileVo.builder()
                .uid(userEntity.getUid())
                .iuser(userEntity.getIuser())
                .address(userEntity.getAddress())
                .addressDetail(userEntity.getAddressDetail())
                .birthday(userEntity.getBirthday())
                .image(userEntity.getImage())
                .mobileNb(userEntity.getMobile_nb())
                .name(userEntity.getName())
                .nickNm(userEntity.getNickNm())
                .zipcode(userEntity.getZipCode())
                .point(userEntity.getPoint())
                .baby(vo)
                .build();
    }
    public ProfileVo profileupdate(ProfileUpdDto dto){
        UserEntity loginUser = USERPK.getLoginUser();
        UserEntity entity = userRep.findById(loginUser.getIuser()).get();

        // "" 이 들어오지 못하도록함
        if (!dto.getNickNm().equals("")){
            entity.setNickNm(dto.getNickNm());
        }
        if (!dto.getUpw().equals("")){
            String encode = PW_ENCODER.encode(dto.getUpw());
            entity.setPassword(encode);
        }
        if (!dto.getPhoneNumber().equals("")) {
            entity.setMobile_nb(dto.getPhoneNumber());
        }
        if (!dto.getUmn().equals("")) {
            entity.setName(dto.getUmn());
        }
        if (!dto.getBirthday().equals("")){
            entity.setBirthday(dto.getBirthday());
        }
        if (!dto.getZipcode().equals("")){
            entity.setZipCode(dto.getZipcode());
        }
        if (!dto.getAddress().equals("")){
            entity.setAddress(dto.getAddress());
        }
        if (!dto.getAddressDetail().equals("")){
            entity.setAddressDetail(dto.getAddressDetail());
        }
        userRep.save(entity);

        return ProfileVo.builder()
                .iuser(entity.getIuser())
                .address(entity.getAddress())
                .addressDetail(entity.getAddressDetail())
                .birthday(entity.getBirthday())
//                .email(entity.getEmail())
                .image(entity.getImage())
                .mobileNb(entity.getMobile_nb())
                .name(entity.getName())
                .nickNm(entity.getNickNm())
                .zipcode(entity.getZipCode())
                .point(entity.getPoint())
                .build();

    }
    public int nicknmcheck(String nickname){
        UserEntity loginUser = USERPK.getLoginUser();
        Long iuser = loginUser.getIuser();
        UserEntity userentity = userRep.findById(iuser).get();

        UserEntity NickNm = userRep.findByNickNm(nickname);

        if (!(null==NickNm)) {
            return 1;
        }
        return 0;
    }
    public void deluser(HttpServletRequest rep,HttpServletResponse res) {
//        String type = appProperties.getAuth().getTokenType();
//        String accessToken = resolveToken(req,type);
//
//        String blackAccessTokenKey = String.format("%s:%s", appProperties.getAuth().getRedisAccessBlackKey(), accessToken);
//        long expiration = authToken.getTokenExpirationTime() - LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
//        if(expiration > 0) {
//            redisService.setValuesWithTimeout(blackAccessTokenKey, "logout", expiration);
//        }
//

        UserEntity user = USERPK.getLoginUser();
        Long iuser = user.getIuser();
        UserEntity userentity = userRep.findById(iuser).get();
        userentity.setDelYn((byte) 1);
        userRep.save(userentity);
    }
    public String resolveToken(HttpServletRequest req, String type) {
        String headerAuth = req.getHeader("authorization");
        return headerAuth != null && headerAuth.startsWith(String.format("%s ", type)) ? headerAuth.substring(type.length()).trim() : null;
    }

    public int selpw(String password){
        UserEntity userId = USERPK.getLoginUser();
        UserEntity userEntity = userRep.findById(userId.getIuser()).get();
        boolean matches = PW_ENCODER.matches(password, userEntity.getPassword());
        if (matches==true){
            return 1;
        }else return 0;
    }

    private final ProductRepository productRep;
    private final SaleVolumnRepository saleRep;
    public SaleVolumnEntity Inssalevolumn(SaleVolumnDto dto){
        ProductEntity productEntity = productRep.findById(dto.getProductId()).get();
        SaleVolumnEntity entity = SaleVolumnEntity.builder().count(dto.getCount()).productId(productEntity).build();

        SaleVolumnEntity save = saleRep.save(entity);

        return save;
    }
    public List<SaleVolumnVo> Selectsale(String year, String month){

        LocalDate start = null;
        LocalDate end ;



        if (month.equals("01")||month.equals("03")||month.equals("05")||month.equals("07")||month.equals("08")||month.equals("10")||month.equals("12")) {
            end = LocalDate.parse(year + "-" + month + "-31");
        } else if (month.equals("02")) {
             end = LocalDate.parse(year+"-"+month+"-28");
         } else
            end = LocalDate.parse(year+"-"+month+"-30");

        if (month.equals("0")){
            start = LocalDate.parse(year+"-01-01");
            end = LocalDate.parse(year+"-12-31");
        }else {
            start = LocalDate.parse(year+"-"+month+"-01");
        }
        log.info("start:{}",start);
        log.info("end:{}",end);

        QSaleVolumnEntity saleVolumn = QSaleVolumnEntity.saleVolumnEntity;

        List<SaleVolumnVo> fetch = jpaQueryFactory.select(Projections.constructor(SaleVolumnVo.class,saleVolumn.productId.productId,saleVolumn.count.sum(),  saleVolumn.productId.pName, saleVolumn.productId.pPrice))
                .from(saleVolumn)
                .where(saleVolumn.createdAt.between(start, end))
                .groupBy(saleVolumn.productId.productId)
                .fetch();

        for (int i = 0; i <fetch.size(); i++) {

            // 상품 가격 가져오기
            Long productId = fetch.get(i).getProductId();
            ProductEntity productEntity = productRep.findById(productId).get();
            int pPrice = productEntity.getPPrice();
            int count = fetch.get(i).getCount();
            int totalprice = count * pPrice;
            fetch.get(i).setPPrice(totalprice);

            //카테고리 단계 붙이기
            ProductCateRelationEntity cate = productcaterelationRep.findByProductEntity(productEntity);

            CategoryEntity categoryEntity = cate.getCategoryEntity();
            Long cateId = categoryEntity.getCateId();
            String name = "[" + cateId + "단계] "+productEntity.getPName();
            fetch.get(i).setPName(name);

        }

        return fetch;
    }
}
