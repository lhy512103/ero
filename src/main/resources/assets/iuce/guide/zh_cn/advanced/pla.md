---
title: 甘蔗制聚乳酸
category: advanced
order: 55
icon: iuce:pla_film_pellet
items: iuce:process_design_table, iuce:process_card, iuce:pla_film_pellet, iuce:pla_injection_pellet, iuce:evaporator, iuce:oligomerization_reactor, iuce:depolymerization_reactor, iuce:crystallizer, iuce:polymerization_reactor, iuce:strand_pelletizer, iuce:catalyst_prep_tank
---

湿磨机用 4 根甘蔗和 500 mB 水制得糖液。发酵罐装乳酸菌种并接冷却水，把糖液转成乳酸发酵液。浓缩罐、低聚釜和解聚釜都需要蒸汽，解聚釜还需要辛酸亚锡催化剂。

{flow: @iuce:evaporator -> @iuce:oligomerization_reactor -> @iuce:depolymerization_reactor -> @iuce:crystallizer -> iuce:lactide}

结晶器接冷却水。每处理 1,000 mB 粗丙交酯，得到 4 个丙交酯和 250 mB 母液。攒够四批母液，送回浓缩罐，可回收 500 mB 乳酸送入低聚釜。为两台产水设备留出工艺水出口。

工艺设计台左槽放空白卡，选择注塑级或薄膜级，点击写入。复制时将原卡放入第二槽。把写好的卡和催化剂插进聚合釜，用冷却水完成聚合与切粒。催化剂配制罐每用 2 份纯净锡粉和 250 mB 乙醇，可制得一份耐久为 128 的催化剂。

{items: iuce:process_design_table, iuce:process_card, iuce:tin_catalyst, iuce:pla_injection_pellet, iuce:pla_film_pellet}
