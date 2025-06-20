CREATE TABLE users (
                       id VARCHAR(36) PRIMARY KEY,
                       provider VARCHAR(10) NOT NULL ,
                       provider_id VARCHAR(100) NOT NULL,
                       user_name_attribute VARCHAR(50) NOT NULL ,
                       name VARCHAR(100) NOT NULL ,
                       role ENUM('ROLE_BUYERS', 'ROLE_SELLERS') ,
                       account_disable BOOLEAN DEFAULT FALSE,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       UNIQUE KEY uk_provider_provider_id (provider, provider_id)
);

-- users와 1대1 관계
CREATE TABLE buyers (
                        user_id    VARCHAR(36) PRIMARY KEY,
                        name_masking_status TINYINT(1) DEFAULT TRUE,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT fk_buyers_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- users와 1대1 관계
CREATE TABLE sellers (
                         user_id         VARCHAR(36) PRIMARY KEY,
                         vendor_name     VARCHAR(100) NOT NULL,
                         vendor_profile_image VARCHAR(255) NOT NULL ,
                         business_number     VARCHAR(20)  NOT NULL,
                         settlement_bank VARCHAR(50),
                         settlement_acc  VARCHAR(30),
                         tags VARCHAR(36),
                         operating_start_time TIME,
                         operating_end_time TIME,
                         closed_days VARCHAR(20),
                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                         updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         CONSTRAINT fk_sellers_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);


-- 주소 불러오는거는 api로 불러와야합니다~ 그 주소를 저장하는 테이블
CREATE TABLE addresses (
                           id VARCHAR(36) PRIMARY KEY,
                           user_id VARCHAR(36) NOT NULL ,
                           title VARCHAR(30) NOT NULL ,
                           city VARCHAR(100) NOT NULL,              -- 시/도
                           district VARCHAR(100) NOT NULL,          -- 시/군/구
                           neighborhood VARCHAR(100) NOT NULL,      -- 읍/면/동
                           street_address VARCHAR(200) NOT NULL,    -- 도로명 주소
                           postal_code VARCHAR(20) NOT NULL ,                  -- 우편번호
                           detail_address VARCHAR(200) NOT NULL ,              -- 상세 주소 (빌딩명, 호수 등)
                           phone_number VARCHAR(30) NOT NULL ,
                           address_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
                           is_default BOOLEAN NOT NULL DEFAULT FALSE,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           CONSTRAINT fk_addresses_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- buyer와 1:N 관계
CREATE TABLE pets (
                      id VARCHAR(36) PRIMARY KEY ,
                      buyer_id VARCHAR(36) NOT NULL,
                      name VARCHAR(100)    NOT NULL,       -- 펫 이름
                      species VARCHAR(50),                 -- 종(개·고양이·기타)
                      breed VARCHAR(100),                  -- 품종
                      age   TINYINT UNSIGNED,              -- 생년월일
                      gender ENUM('M','F'),                -- 성별
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      CONSTRAINT fk_pets_buyers_id FOREIGN KEY (buyer_id) REFERENCES buyers(user_id) ON DELETE CASCADE
);


CREATE TABLE products (
                          id VARCHAR(36) PRIMARY KEY ,
                          product_number BIGINT NOT NULL,
    -- 유저에게 보여지는 상품 아이디 숫자 형태로 보여질 예정 id랑 다르게 uuid 아님 별도의 로직으로 구성해야함
                          seller_id VARCHAR(36) NOT NULL ,
                          title VARCHAR(50) NOT NULL ,
                          contents TEXT NOT NULL ,
                          category ENUM('dog', 'cat'),
                          stock_status ENUM('IN_STOCK', 'OUT_OF_STOCK'),
                          is_discounted TINYINT(1) DEFAULT FALSE,
                          discount_rate DECIMAL(10,2) DEFAULT 0.00,
                          price BIGINT NOT NULL ,
                          quantity INT UNSIGNED NOT NULL ,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          UNIQUE uk_products_product_name (product_number),
                          CONSTRAINT fk_products_seller_id FOREIGN KEY (seller_id) REFERENCES sellers(user_id)
);

-- 1. 재고 조정 로그 테이블 생성
CREATE TABLE inventory_adjustments (
                                       id                VARCHAR(36) PRIMARY KEY,
                                       product_id        VARCHAR(36) NOT NULL,
                                       adjustment_type   ENUM(
                                           'IN',        -- 입고
                                           'OUT',       -- 출고
                                           'RETURN',    -- 반품
                                           'DISPOSE',   -- 폐기
                                           'ADJUSTMENT' -- 기타 재고 조정
                                           ) NOT NULL,
                                       quantity          INT NOT NULL,           -- 조정 수량 (음수 불가, 타입에 따라 처리)
                                       note              VARCHAR(255),           -- 조정 사유나 메모
                                       created_by        VARCHAR(36),            -- 조정 작업자 (optional)
                                       created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
                                       CONSTRAINT fk_inv_adj_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- 2. products 테이블에 현재 재고를 관리하는 컬럼이 이미 있다면,
--    재고 변경 시 이 로그를 기록하고 products.quantity를 업데이트하면 됩니다.
--    (products.quantity 컬럼은 이미 존재하므로 별도 수정은 필요 없습니다.)

-- 3. 예시 트리거 (MySQL) — 로그를 남기고 자동으로 products.quantity를 업데이트
DELIMITER //
CREATE TRIGGER trg_after_inventory_adjustment
    AFTER INSERT ON inventory_adjustments
    FOR EACH ROW
BEGIN
    UPDATE products
    SET quantity = CASE
                       WHEN NEW.adjustment_type IN ('IN', 'RETURN') THEN quantity + NEW.quantity
                       WHEN NEW.adjustment_type IN ('OUT', 'DISPOSE') THEN quantity - NEW.quantity
                       WHEN NEW.adjustment_type = 'ADJUSTMENT' THEN NEW.quantity  -- 보정일 경우, 절대값 대체
                       ELSE quantity
        END,
        updated_at = NOW()
    WHERE id = NEW.product_id;
END;
//
DELIMITER ;



-- 상품과 1대 다 관계
CREATE TABLE reviews (
                         id VARCHAR(36) PRIMARY KEY,
                         product_id VARCHAR(36) NOT NULL,
                         buyer_id VARCHAR(36) NOT NULL,
                         star DECIMAL(2,1) NOT NULL,
                         contents TEXT NOT NULL,
                         summary VARCHAR(100) NULL ,
                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                         updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         CONSTRAINT fk_reviews_product_id FOREIGN KEY (product_id) REFERENCES products(id),
                         CONSTRAINT fk_reviews_buyer_id FOREIGN KEY (buyer_id) REFERENCES buyers(user_id)
);

CREATE TABLE reviews_summary_llm (
                                     id VARCHAR(36) PRIMARY KEY ,
                                     product_id VARCHAR(36) NOT NULL ,
                                     positive_review TEXT NOT NULL ,
                                     negative_review TEXT NOT NULL ,
                                     CONSTRAINT fk_reviews_summary_llm_product_id FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE images (
                        id VARCHAR(36) PRIMARY KEY,
                        image_url VARCHAR(255) NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 이미지 매핑 테이블
CREATE TABLE products_images (
                                 id VARCHAR(36) PRIMARY KEY,
                                 product_id VARCHAR(36) NOT NULL,
                                 product_image_id VARCHAR(36) NOT NULL,
                                 created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_products_images_product_id FOREIGN KEY (product_id) REFERENCES products(id),
                                 CONSTRAINT fk_products_images_image_id FOREIGN KEY (product_image_id) REFERENCES images(id)
);

-- 이미지 매핑 테이블
CREATE TABLE reviews_images (
                                id VARCHAR(36) PRIMARY KEY ,
                                review_image_id VARCHAR(36) NOT NULL,
                                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                CONSTRAINT fk_reviews_images_review_id FOREIGN KEY (id) REFERENCES reviews(id),
                                CONSTRAINT fk_reviews_images_image_id FOREIGN KEY (review_image_id) REFERENCES images(id)
);

-- 주문 정보 테아블이고 장바구니도 포함입니다!
-- PAYMENT_PENDING 상태가 장바구니에 있을 때 상태에요
CREATE TABLE orders (
                        id BIGINT PRIMARY KEY,
                        user_id VARCHAR(36) NOT NULL,
                        order_status ENUM(
                            'PAYMENT_COMPLETED',
                            'PREPARING',
                            'READY_FOR_SHIPMENT',
                            'IN_DELIVERY',
                            'DELIVERED'
                            ) ,
                        total_price BIGINT NOT NULL,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT fk_orders_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);


-- 장바구니 헤더
CREATE TABLE carts (
                       id           VARCHAR(36) PRIMARY KEY,
                       user_id      VARCHAR(36) NOT NULL ,
                       created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 장바구니 아이템
CREATE TABLE cart_items (
                            id         VARCHAR(36) PRIMARY KEY,
                            cart_id    VARCHAR(36) NOT NULL,
                            product_id VARCHAR(36) NOT NULL,
                            quantity   INT UNSIGNED NOT NULL,
                            added_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
                            CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);


CREATE TABLE orders_compare_llm (
                                    id VARCHAR(36) PRIMARY KEY ,
                                    buyer_id VARCHAR(36) NOT NULL ,
                                    order_id VARCHAR(36) NOT NULL ,
                                    category ENUM('dog', 'cat'),
                                    contents TEXT NOT NULL ,
                                    CONSTRAINT fk_products_compare_llm_order_id FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- 주문 상세 페이지에 사용되는것!
CREATE TABLE order_items (
                             id VARCHAR(36) PRIMARY KEY,
                             order_id BIGINT NOT NULL, -- 주문 번호는 유저에게 보이기 때문에 숫자로
                             product_id VARCHAR(36) NOT NULL,
                             quantity INT UNSIGNED NOT NULL,
                             price BIGINT NOT NULL,  -- 주문 시점 상품 가격 저장 (변경 대비)
                             CONSTRAINT fk_order_items_order_id FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                             CONSTRAINT fk_order_items_product_id FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE payments (
                          id VARCHAR(36) PRIMARY KEY,
                          buyers_id VARCHAR(36) NOT NULL ,
                          order_id BIGINT NOT NULL,
                          method ENUM('TOSS') NOT NULL,
                          status ENUM('PENDING', 'SUCCESS', 'FAILED') DEFAULT 'PENDING',
                          toss_payment_key VARCHAR(255) NOT NULL ,
                          paid_at DATETIME,
                          CONSTRAINT fk_payments_user_id FOREIGN KEY (buyers_id) REFERENCES buyers(user_id) ,
                          CONSTRAINT fk_payments_order_id FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE refunds (
                         id VARCHAR(36) PRIMARY KEY,
                         payment_id VARCHAR(36) NOT NULL,
                         order_issue_id VARCHAR(36) NULL, -- 추가
                         buyer_id VARCHAR(36) NOT NULL ,
                         reason TEXT,
                         refunded_at DATETIME,
                         CONSTRAINT fk_refunds_payment_id FOREIGN KEY (payment_id) REFERENCES payments(id),
                         CONSTRAINT fk_refunds_order_issue FOREIGN KEY (order_issue_id) REFERENCES order_issues(id) ON DELETE SET NULL
);

CREATE TABLE coupons (
                         id VARCHAR(36) PRIMARY KEY,
                         code VARCHAR(50) NOT NULL,
                         discount_type ENUM('PERCENT', 'AMOUNT') NOT NULL,
                         discount_value INT NOT NULL,
                         expires_at DATETIME,
                         usage_limit INT,
                         used_count INT DEFAULT 0
);

CREATE TABLE user_coupons (
                              id VARCHAR(36) PRIMARY KEY,
                              user_id VARCHAR(36) NOT NULL,
                              coupon_id VARCHAR(36) NOT NULL,
                              is_used BOOLEAN NOT NULL DEFAULT FALSE,
                              used_at TIMESTAMP NULL,
                              CONSTRAINT fk_user_coupons_user_id FOREIGN KEY (user_id) REFERENCES buyers(user_id) ,
                              CONSTRAINT fk_user_coupons_coupon_id FOREIGN KEY (coupon_id) REFERENCES coupons(id)
);


-- 주문 이슈(취소, 교환, 반품, 환불 등) 요청 및 처리 이력
CREATE TABLE order_issues (
                              id VARCHAR(36) PRIMARY KEY,
                              order_id BIGINT NOT NULL,
                              buyer_id VARCHAR(36) NOT NULL, -- 추가
                              issue_request_number VARCHAR(50) NULL, -- 추가
                              issue_type ENUM('CANCEL', 'REFUND') NOT NULL,
                              issue_status ENUM('REQUESTED', 'APPROVED', 'COMPLETED', 'REJECTED') DEFAULT 'REQUESTED',
                              issue_request_date DATETIME NOT NULL,
                              issue_complete_date DATETIME NULL,
                              total_amount BIGINT NOT NULL, -- 변경
                              delivery_fee BIGINT DEFAULT 0, -- 추가
                              discount_amount BIGINT DEFAULT 0, -- 추가
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              CONSTRAINT fk_order_issues_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                              CONSTRAINT fk_order_issues_buyer FOREIGN KEY (buyer_id) REFERENCES buyers(user_id) ON DELETE CASCADE
);


-- 주문 이슈에 포함된 개별 상품 정보 (교환/반품/취소할 상품 단위)
-- 예시로 상품을 한 번에 4개 다른 상품을 주문 했고 그 주문 내역에 부분적으로 상품을 환불한다면?
-- 상세 페이지에 들어가면 그에대한 정보가 필요하겠죠?
CREATE TABLE order_issue_items (
                                   id VARCHAR(36) PRIMARY KEY,
                                   seller_id VARCHAR(36) NOT NULL,  -- 판매자 정보 추가 한 주문에 여러판매자가 있을 수 있기 때문에
    -- 반품, 환불, 교환 등 이슈 처리 시 각 판매자별 처리나 정산시 필요하게 됨
                                   order_issue_id VARCHAR(36) NOT NULL,
                                   order_item_id VARCHAR(36) NOT NULL,
                                   quantity INT UNSIGNED NOT NULL,

    -- 요청 시점 상품 가격 (정확한 환불금 계산을 위해)
                                   item_price BIGINT NOT NULL,


    -- 개별 상품 이슈 상태
                                   status ENUM('REQUESTED', 'APPROVED', 'COMPLETED', 'REJECTED') DEFAULT 'REQUESTED',
                                   issue_reason TEXT,
                                   refund_method VARCHAR(50),
                                   refund_amount BIGINT DEFAULT 0,
                                   delivery_fee BIGINT DEFAULT 0,
                                   return_fee BIGINT DEFAULT 0,
                                   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                   updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   CONSTRAINT fk_order_issue_items_seller_id FOREIGN KEY (seller_id) REFERENCES sellers(user_id),
                                   CONSTRAINT fk_order_issue_items_issue FOREIGN KEY (order_issue_id) REFERENCES order_issues(id) ON DELETE CASCADE,
                                   CONSTRAINT fk_order_issue_items_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE CASCADE
);


-- 배송 추적은 api를 연동하기 때문에 우리 db는 간단히 저장
CREATE TABLE shipments (
                           id VARCHAR(36) PRIMARY KEY,
                           order_id VARCHAR(36) NOT NULL,
                           seller_id VARCHAR(36) NOT NULL,
                           courier VARCHAR(50) NOT NULL,            -- 택배사 이름 (e.g., CJ, 로젠, 우체국)
                           tracking_number VARCHAR(100) NOT NULL,   -- 송장 번호
                           shipped_at DATETIME,                     -- 발송일자 (선택)
                           delivered_at DATETIME,                   -- 배송 완료일자 (API 연동 시 업데이트 가능)
                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                           updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                           CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                           CONSTRAINT fk_shipments_seller FOREIGN KEY (seller_id) REFERENCES sellers(user_id)
);

-- 정산 테이블이고 이 테이블은 좀 더 고민해봐야함 사유 : 정산 로직을 정하지 않았기 때문
CREATE TABLE settlements (
                             id VARCHAR(36) PRIMARY KEY,
                             seller_id VARCHAR(36) NOT NULL,
                             order_item_id VARCHAR(36) NOT NULL,  -- 어떤 상품 주문인지
                             item_price BIGINT NOT NULL,          -- 실제 판매가
                             commission_rate DECIMAL(5,2) NOT NULL, -- 예: 10.00 (%)
                             commission_amount BIGINT NOT NULL,   -- 수수료 금액 (계산된 값)
                             settlement_amount BIGINT NOT NULL,   -- 정산 금액 (item_price - commission_amount)
                             settlement_status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED') DEFAULT 'PENDING',
                             settled_at DATETIME,
                             created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                             updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                             CONSTRAINT fk_settlements_seller FOREIGN KEY (seller_id) REFERENCES sellers(user_id),
                             CONSTRAINT fk_settlements_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id)
);


-- 팜매자 대시보드는 view 테이블로 구성합니다
-- 매번 서버에 요청하게되면 매번 페이지 접속마다 집계함수 쿼리가 나가 성능이
-- 엄청 구려요 그렇기 때문에 물리 view를 구현해서 부하를 db로 분산해야합니다


-- 알림 기능 테이블
CREATE TABLE notifications (
                               id VARCHAR(36) PRIMARY KEY,
                               user_id VARCHAR(36) NOT NULL,
                               type ENUM(
                                   'ORDER_STATUS_CHANGED',
                                   'DELIVERY_STARTED',
                                   'COUPON_ISSUED',
                                   'REVIEW_REQUEST',
                                   'SYSTEM_ALERT',
                                   'REFUND_COMPLETED',
                                   'CHATTING'
                                   ) NOT NULL,
                               title VARCHAR(100) NOT NULL,
                               message TEXT NOT NULL,
                               is_read BOOLEAN DEFAULT FALSE, -- 읽었는지 여부
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               read_at DATETIME NULL, -- 정확히 언제 읽었는지 시간 정보
    -- 추후 확장에 유리하기 때문에 분리 -> ex) kafka -> 하지만 우리는 쪼렙이라 redis만 쓸 예정! 이유는 kafka 면접 질문 받아칠 수 없을거기 때문~
                               CONSTRAINT fk_notifications_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);


-- 채팅방 테이블 (판매자-구매자 1:1 기준)
CREATE TABLE chat_rooms (
                            id VARCHAR(36) PRIMARY KEY,
                            buyer_id VARCHAR(36) NOT NULL,
                            seller_id VARCHAR(36) NOT NULL,
                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT uk_chat_rooms UNIQUE (buyer_id, seller_id),
                            CONSTRAINT fk_chat_rooms_buyer FOREIGN KEY (buyer_id) REFERENCES buyers(user_id),
                            CONSTRAINT fk_chat_rooms_seller FOREIGN KEY (seller_id) REFERENCES sellers(user_id)
);

-- 메시지 테이블
-- redis랑 같이 사용 예정~
-- 메시지를 받자마자 Redis에 저장하고, 비동기적으로 MySQL에 저장
CREATE TABLE chat_messages (
                               id VARCHAR(36) PRIMARY KEY,
                               room_id VARCHAR(36) NOT NULL,
                               sender_id VARCHAR(36) NOT NULL,
                               message TEXT NOT NULL,
                               is_read BOOLEAN DEFAULT FALSE,
                               sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               read_at DATETIME,
                               CONSTRAINT fk_chat_messages_room FOREIGN KEY (room_id) REFERENCES chat_rooms(id),
    -- sender_id는 buyers/sellers 어느 쪽이든 가능하므로 제약은 논리적으로만 처리
                               INDEX idx_chat_messages_room_id (room_id),
                               INDEX idx_chat_messages_sender_id (sender_id)
);

CREATE TABLE reports (
                         id VARCHAR(36) AUTO_INCREMENT PRIMARY KEY,
                         report_type ENUM('product', 'review') NOT NULL,
                         target_id VARCHAR(36) NOT NULL, -- product_id 또는 review_id
                         reporter_id VARCHAR(36) NOT NULL,
                         reason VARCHAR(255) NOT NULL,
                         status ENUM('pending', 'completed') NOT NULL DEFAULT 'pending',
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         processed_at TIMESTAMP NULL,
                         CONSTRAINT fk_reports_reporter_id FOREIGN KEY (reporter_id) REFERENCES buyers(user_id)
);

-- 관리자 페이지는 내부망으로 외부 접근 차단 따라서 어드민 테이블은 따로둠
CREATE TABLE admins (
                        id VARCHAR(36) PRIMARY KEY ,
                        role ENUM('ROLE_ADMIN'),
                        email VARCHAR(50) NOT NULL ,
                        password VARCHAR(255) NOT NULL ,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


CREATE TABLE notices (
                         id VARCHAR(36) PRIMARY KEY,
                         title VARCHAR(255) NOT NULL,
                         content TEXT NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE faqs (
                      id VARCHAR(36) PRIMARY KEY,
                      question VARCHAR(255) NOT NULL,
                      answer TEXT NOT NULL,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE inquiries (
                           id VARCHAR(36) PRIMARY KEY,
                           user_id VARCHAR(36) NOT NULL,
                           parent_id VARCHAR(36) NULL,
                           admin_id VARCHAR(36) NULL,
                           title VARCHAR(255) NULL,
                           content TEXT NOT NULL,
                           status ENUM('pending', 'answered') NOT NULL DEFAULT 'pending',
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           FOREIGN KEY (user_id) REFERENCES users(id),
                           FOREIGN KEY (parent_id) REFERENCES inquiries(id) ON DELETE SET NULL,
                           FOREIGN KEY (admin_id) REFERENCES admins(id)
);

CREATE TABLE files (
                       id VARCHAR(36) PRIMARY KEY ,
                       file_url VARCHAR(255) NOT NULL
);

CREATE TABLE inquiry_files (
                               id VARCHAR(36) PRIMARY KEY,
                               inquiry_id VARCHAR(36) NOT NULL ,
                               CONSTRAINT fk_inquiry_files_inquiry_id FOREIGN KEY (inquiry_id) REFERENCES inquiries(id)
);

CREATE TABLE notice_files (
                              id VARCHAR(36) PRIMARY KEY ,
                              notice_id VARCHAR(36) NOT NULL ,
                              CONSTRAINT fk_notice_files_notice_id FOREIGN KEY (notice_id) REFERENCES notices(id)
);