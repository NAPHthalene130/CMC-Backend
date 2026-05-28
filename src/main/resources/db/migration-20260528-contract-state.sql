USE cmc;

ALTER TABLE `contract`
    ADD COLUMN `state` INT NOT NULL DEFAULT 1 COMMENT '当前状态：1起草 2会签完成 3定稿完成 4审批完成 5签订完成'
    AFTER `template_id`;

UPDATE `contract` c
LEFT JOIN (
    SELECT cs.contract_id, cs.type
    FROM contract_state cs
    INNER JOIN (
        SELECT contract_id, MAX(id) AS max_id
        FROM contract_state
        GROUP BY contract_id
    ) latest ON latest.max_id = cs.id
) s ON s.contract_id = c.id
SET c.state = COALESCE(s.type, 1);
