-- V9: fix unit from 'м2' to 'м.п.' for banner/film materials and back-fill default dimensions.
-- Items 1-3 (banner/film) are sold by the running metre; height is unused (= 0).
-- Item 4 (плитная плёнка) was also treated as 'м.п.' in the draft V4, so keep it consistent.

UPDATE ordschema.materials
SET   unit              = 'м.п.',
      default_width_mm  = CASE id
                             WHEN 1  THEN 1600  -- Баннер Frontlit 440 г/м²  — рулон 1.60 м
                             WHEN 2  THEN 1070  -- Баннер Blockout 510 г/м² — рулон 1.07 м
                             WHEN 3  THEN 1370  -- Плёнка monomer 80 мкм    — рулон 1.37 м
                             WHEN 4  THEN 1370  -- Плёнка cast 100 мкм      — рулон 1.37 м
                             ELSE default_width_mm
                           END,
      default_height_mm = 0
WHERE id IN (1, 2, 3, 4);
