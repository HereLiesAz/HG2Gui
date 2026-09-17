# HG2Gui animation TODO

This file is generated from `docs/guide/MANIFEST.json` by `scripts/build_animation_todo.py`.
It audits every animation clip/scene for both a START and END storyboard boundary.
A checked animation means every start/end boundary used by every clip is backed by an actual image file in the organized Guide tree.

## Current coverage

| Group | Animations | Clips / scenes | Boundary slots | Materialized | Missing / unresolved |
| --- | ---: | ---: | ---: | ---: | ---: |
| Canonical | 35 | 117 | 234 | 8 | 226 |
| Tangents | 4 | 42 | 84 | 80 | 4 |
| Legacy | 3 | 14 | 28 | 28 | 0 |
| **Total** | **42** | **173** | **346** | **116** | **230** |

Canonical production uses **152 unique boundary image filenames** across the canonical clips because adjacent clips share boundaries. **5/152 unique canonical frames are currently present.**

## Boundary rules

- Every clip must have both a START and END image.
- When clip N END is clip N+1 START, it is the **same image**, copied into both scene folders by the organizer.
- A declared filename without an image is not considered complete.
- Missing frames are listed below; no substitute artwork is fabricated.
- Existing source artwork may be canonicalized through `docs/animation_frame_aliases.json`; run `scripts/materialize_animation_frame_aliases.py`, then `scripts/organize_guide_material.py`, then this script.

## Canonical animations

### [ ] 01 — `pwd`

Unique canonical boundary frames: **0/5 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `01_pwd_frame_00.png` — missing | ⬜ `01_pwd_frame_01.png` — missing |
| 02 | ⬜ `01_pwd_frame_01.png` — missing | ⬜ `01_pwd_frame_02.png` — missing |
| 03 | ⬜ `01_pwd_frame_02.png` — missing | ⬜ `01_pwd_frame_03.png` — missing |
| 04 | ⬜ `01_pwd_frame_03.png` — missing | ⬜ `01_pwd_frame_04.png` — missing |

**TODO — missing boundaries**

- [ ] `01_pwd_frame_00.png`
- [ ] `01_pwd_frame_01.png`
- [ ] `01_pwd_frame_02.png`
- [ ] `01_pwd_frame_03.png`
- [ ] `01_pwd_frame_04.png`

### [ ] 02 — `pkg install`

Unique canonical boundary frames: **4/5 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ✅ `02_pkg_install_frame_00.png` | ✅ `02_pkg_install_frame_01.png` |
| 02 | ✅ `02_pkg_install_frame_01.png` | ⬜ `02_pkg_install_frame_02.png` — missing |
| 03 | ⬜ `02_pkg_install_frame_02.png` — missing | ✅ `02_pkg_install_frame_03.png` |
| 04 | ✅ `02_pkg_install_frame_03.png` | ✅ `02_pkg_install_frame_04.png` |

**TODO — missing boundaries**

- [ ] `02_pkg_install_frame_02.png`

### [ ] 03 — `apt-get update`

Unique canonical boundary frames: **1/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `03_apt_get_update_frame_00.png` — missing | ✅ `03_apt_get_update_frame_01.png` |
| 02 | ✅ `03_apt_get_update_frame_01.png` | ⬜ `03_apt_get_update_frame_02.png` — missing |
| 03 | ⬜ `03_apt_get_update_frame_02.png` — missing | ⬜ `03_apt_get_update_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `03_apt_get_update_frame_00.png`
- [ ] `03_apt_get_update_frame_02.png`
- [ ] `03_apt_get_update_frame_03.png`

### [ ] 04 — `nano`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `04_nano_frame_00.png` — missing | ⬜ `04_nano_frame_01.png` — missing |
| 02 | ⬜ `04_nano_frame_01.png` — missing | ⬜ `04_nano_frame_02.png` — missing |
| 03 | ⬜ `04_nano_frame_02.png` — missing | ⬜ `04_nano_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `04_nano_frame_00.png`
- [ ] `04_nano_frame_01.png`
- [ ] `04_nano_frame_02.png`
- [ ] `04_nano_frame_03.png`

### [ ] 05 — `top`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `05_top_frame_00.png` — missing | ⬜ `05_top_frame_01.png` — missing |
| 02 | ⬜ `05_top_frame_01.png` — missing | ⬜ `05_top_frame_02.png` — missing |
| 03 | ⬜ `05_top_frame_02.png` — missing | ⬜ `05_top_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `05_top_frame_00.png`
- [ ] `05_top_frame_01.png`
- [ ] `05_top_frame_02.png`
- [ ] `05_top_frame_03.png`

### [ ] 06 — `kill`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `06_kill_frame_00.png` — missing | ⬜ `06_kill_frame_01.png` — missing |
| 02 | ⬜ `06_kill_frame_01.png` — missing | ⬜ `06_kill_frame_02.png` — missing |
| 03 | ⬜ `06_kill_frame_02.png` — missing | ⬜ `06_kill_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `06_kill_frame_00.png`
- [ ] `06_kill_frame_01.png`
- [ ] `06_kill_frame_02.png`
- [ ] `06_kill_frame_03.png`

### [ ] 07 — `df`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `07_df_frame_00.png` — missing | ⬜ `07_df_frame_01.png` — missing |
| 02 | ⬜ `07_df_frame_01.png` — missing | ⬜ `07_df_frame_02.png` — missing |
| 03 | ⬜ `07_df_frame_02.png` — missing | ⬜ `07_df_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `07_df_frame_00.png`
- [ ] `07_df_frame_01.png`
- [ ] `07_df_frame_02.png`
- [ ] `07_df_frame_03.png`

### [ ] 08 — `rm`

Unique canonical boundary frames: **0/5 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `08_rm_frame_00.png` — missing | ⬜ `08_rm_frame_01.png` — missing |
| 02 | ⬜ `08_rm_frame_01.png` — missing | ⬜ `08_rm_frame_02.png` — missing |
| 03 | ⬜ `08_rm_frame_02.png` — missing | ⬜ `08_rm_frame_03.png` — missing |
| 04 | ⬜ `08_rm_frame_03.png` — missing | ⬜ `08_rm_frame_04.png` — missing |

**TODO — missing boundaries**

- [ ] `08_rm_frame_00.png`
- [ ] `08_rm_frame_01.png`
- [ ] `08_rm_frame_02.png`
- [ ] `08_rm_frame_03.png`
- [ ] `08_rm_frame_04.png`

### [ ] 09 — `source`

Unique canonical boundary frames: **0/5 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `09_source_frame_00.png` — missing | ⬜ `09_source_frame_01.png` — missing |
| 02 | ⬜ `09_source_frame_01.png` — missing | ⬜ `09_source_frame_02.png` — missing |
| 03 | ⬜ `09_source_frame_02.png` — missing | ⬜ `09_source_frame_03.png` — missing |
| 04 | ⬜ `09_source_frame_03.png` — missing | ⬜ `09_source_frame_04.png` — missing |

**TODO — missing boundaries**

- [ ] `09_source_frame_00.png`
- [ ] `09_source_frame_01.png`
- [ ] `09_source_frame_02.png`
- [ ] `09_source_frame_03.png`
- [ ] `09_source_frame_04.png`

### [ ] 10 — `ping`

Unique canonical boundary frames: **0/5 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `10_ping_frame_00.png` — missing | ⬜ `10_ping_frame_01.png` — missing |
| 02 | ⬜ `10_ping_frame_01.png` — missing | ⬜ `10_ping_frame_02.png` — missing |
| 03 | ⬜ `10_ping_frame_02.png` — missing | ⬜ `10_ping_frame_03.png` — missing |
| 04 | ⬜ `10_ping_frame_03.png` — missing | ⬜ `10_ping_frame_04.png` — missing |

**TODO — missing boundaries**

- [ ] `10_ping_frame_00.png`
- [ ] `10_ping_frame_01.png`
- [ ] `10_ping_frame_02.png`
- [ ] `10_ping_frame_03.png`
- [ ] `10_ping_frame_04.png`

### [ ] 11 — `ssh`

Unique canonical boundary frames: **0/5 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `11_ssh_frame_00.png` — missing | ⬜ `11_ssh_frame_01.png` — missing |
| 02 | ⬜ `11_ssh_frame_01.png` — missing | ⬜ `11_ssh_frame_02.png` — missing |
| 03 | ⬜ `11_ssh_frame_02.png` — missing | ⬜ `11_ssh_frame_03.png` — missing |
| 04 | ⬜ `11_ssh_frame_03.png` — missing | ⬜ `11_ssh_frame_04.png` — missing |

**TODO — missing boundaries**

- [ ] `11_ssh_frame_00.png`
- [ ] `11_ssh_frame_01.png`
- [ ] `11_ssh_frame_02.png`
- [ ] `11_ssh_frame_03.png`
- [ ] `11_ssh_frame_04.png`

### [ ] 12 — `harden-check`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `12_harden_check_frame_00.png` — missing | ⬜ `12_harden_check_frame_01.png` — missing |
| 02 | ⬜ `12_harden_check_frame_01.png` — missing | ⬜ `12_harden_check_frame_02.png` — missing |
| 03 | ⬜ `12_harden_check_frame_02.png` — missing | ⬜ `12_harden_check_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `12_harden_check_frame_00.png`
- [ ] `12_harden_check_frame_01.png`
- [ ] `12_harden_check_frame_02.png`
- [ ] `12_harden_check_frame_03.png`

### [ ] 13 — `git commit`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `13_git_commit_frame_00.png` — missing | ⬜ `13_git_commit_frame_01.png` — missing |
| 02 | ⬜ `13_git_commit_frame_01.png` — missing | ⬜ `13_git_commit_frame_02.png` — missing |
| 03 | ⬜ `13_git_commit_frame_02.png` — missing | ⬜ `13_git_commit_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `13_git_commit_frame_00.png`
- [ ] `13_git_commit_frame_01.png`
- [ ] `13_git_commit_frame_02.png`
- [ ] `13_git_commit_frame_03.png`

### [ ] 14 — `nmap`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `14_nmap_frame_00.png` — missing | ⬜ `14_nmap_frame_01.png` — missing |
| 02 | ⬜ `14_nmap_frame_01.png` — missing | ⬜ `14_nmap_frame_02.png` — missing |
| 03 | ⬜ `14_nmap_frame_02.png` — missing | ⬜ `14_nmap_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `14_nmap_frame_00.png`
- [ ] `14_nmap_frame_01.png`
- [ ] `14_nmap_frame_02.png`
- [ ] `14_nmap_frame_03.png`

### [ ] 15 — `crond`

Unique canonical boundary frames: **0/5 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `15_crond_frame_00.png` — missing | ⬜ `15_crond_frame_01.png` — missing |
| 02 | ⬜ `15_crond_frame_01.png` — missing | ⬜ `15_crond_frame_02.png` — missing |
| 03 | ⬜ `15_crond_frame_02.png` — missing | ⬜ `15_crond_frame_03.png` — missing |
| 04 | ⬜ `15_crond_frame_03.png` — missing | ⬜ `15_crond_frame_04.png` — missing |

**TODO — missing boundaries**

- [ ] `15_crond_frame_00.png`
- [ ] `15_crond_frame_01.png`
- [ ] `15_crond_frame_02.png`
- [ ] `15_crond_frame_03.png`
- [ ] `15_crond_frame_04.png`

### [ ] 16 — `watch`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `16_watch_frame_00.png` — missing | ⬜ `16_watch_frame_01.png` — missing |
| 02 | ⬜ `16_watch_frame_01.png` — missing | ⬜ `16_watch_frame_02.png` — missing |
| 03 | ⬜ `16_watch_frame_02.png` — missing | ⬜ `16_watch_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `16_watch_frame_00.png`
- [ ] `16_watch_frame_01.png`
- [ ] `16_watch_frame_02.png`
- [ ] `16_watch_frame_03.png`

### [ ] 17 — `AI chat`

Unique canonical boundary frames: **0/5 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `17_ai_chat_frame_00.png` — missing | ⬜ `17_ai_chat_frame_01.png` — missing |
| 02 | ⬜ `17_ai_chat_frame_01.png` — missing | ⬜ `17_ai_chat_frame_02.png` — missing |
| 03 | ⬜ `17_ai_chat_frame_02.png` — missing | ⬜ `17_ai_chat_frame_03.png` — missing |
| 04 | ⬜ `17_ai_chat_frame_03.png` — missing | ⬜ `17_ai_chat_frame_04.png` — missing |

**TODO — missing boundaries**

- [ ] `17_ai_chat_frame_00.png`
- [ ] `17_ai_chat_frame_01.png`
- [ ] `17_ai_chat_frame_02.png`
- [ ] `17_ai_chat_frame_03.png`
- [ ] `17_ai_chat_frame_04.png`

### [ ] 18 — `skill`

Unique canonical boundary frames: **0/5 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `18_skill_frame_00.png` — missing | ⬜ `18_skill_frame_01.png` — missing |
| 02 | ⬜ `18_skill_frame_01.png` — missing | ⬜ `18_skill_frame_02.png` — missing |
| 03 | ⬜ `18_skill_frame_02.png` — missing | ⬜ `18_skill_frame_03.png` — missing |
| 04 | ⬜ `18_skill_frame_03.png` — missing | ⬜ `18_skill_frame_04.png` — missing |

**TODO — missing boundaries**

- [ ] `18_skill_frame_00.png`
- [ ] `18_skill_frame_01.png`
- [ ] `18_skill_frame_02.png`
- [ ] `18_skill_frame_03.png`
- [ ] `18_skill_frame_04.png`

### [ ] 19 — `ls -a`

Unique canonical boundary frames: **0/5 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `19_ls_a_frame_00.png` — missing | ⬜ `19_ls_a_frame_01.png` — missing |
| 02 | ⬜ `19_ls_a_frame_01.png` — missing | ⬜ `19_ls_a_frame_02.png` — missing |
| 03 | ⬜ `19_ls_a_frame_02.png` — missing | ⬜ `19_ls_a_frame_03.png` — missing |
| 04 | ⬜ `19_ls_a_frame_03.png` — missing | ⬜ `19_ls_a_frame_04.png` — missing |

**TODO — missing boundaries**

- [ ] `19_ls_a_frame_00.png`
- [ ] `19_ls_a_frame_01.png`
- [ ] `19_ls_a_frame_02.png`
- [ ] `19_ls_a_frame_03.png`
- [ ] `19_ls_a_frame_04.png`

### [ ] 20 — `sudo`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `20_sudo_frame_00.png` — missing | ⬜ `20_sudo_frame_01.png` — missing |
| 02 | ⬜ `20_sudo_frame_01.png` — missing | ⬜ `20_sudo_frame_02.png` — missing |
| 03 | ⬜ `20_sudo_frame_02.png` — missing | ⬜ `20_sudo_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `20_sudo_frame_00.png`
- [ ] `20_sudo_frame_01.png`
- [ ] `20_sudo_frame_02.png`
- [ ] `20_sudo_frame_03.png`

### [ ] 21 — `rmdir`

Unique canonical boundary frames: **0/5 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `21_rmdir_frame_00.png` — missing | ⬜ `21_rmdir_frame_01.png` — missing |
| 02 | ⬜ `21_rmdir_frame_01.png` — missing | ⬜ `21_rmdir_frame_02.png` — missing |
| 03 | ⬜ `21_rmdir_frame_02.png` — missing | ⬜ `21_rmdir_frame_03.png` — missing |
| 04 | ⬜ `21_rmdir_frame_03.png` — missing | ⬜ `21_rmdir_frame_04.png` — missing |

**TODO — missing boundaries**

- [ ] `21_rmdir_frame_00.png`
- [ ] `21_rmdir_frame_01.png`
- [ ] `21_rmdir_frame_02.png`
- [ ] `21_rmdir_frame_03.png`
- [ ] `21_rmdir_frame_04.png`

### [ ] 22 — `find`

Unique canonical boundary frames: **0/5 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `22_find_frame_00.png` — missing | ⬜ `22_find_frame_01.png` — missing |
| 02 | ⬜ `22_find_frame_01.png` — missing | ⬜ `22_find_frame_02.png` — missing |
| 03 | ⬜ `22_find_frame_02.png` — missing | ⬜ `22_find_frame_03.png` — missing |
| 04 | ⬜ `22_find_frame_03.png` — missing | ⬜ `22_find_frame_04.png` — missing |

**TODO — missing boundaries**

- [ ] `22_find_frame_00.png`
- [ ] `22_find_frame_01.png`
- [ ] `22_find_frame_02.png`
- [ ] `22_find_frame_03.png`
- [ ] `22_find_frame_04.png`

### [ ] 23 — `cat`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `23_cat_frame_00.png` — missing | ⬜ `23_cat_frame_01.png` — missing |
| 02 | ⬜ `23_cat_frame_01.png` — missing | ⬜ `23_cat_frame_02.png` — missing |
| 03 | ⬜ `23_cat_frame_02.png` — missing | ⬜ `23_cat_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `23_cat_frame_00.png`
- [ ] `23_cat_frame_01.png`
- [ ] `23_cat_frame_02.png`
- [ ] `23_cat_frame_03.png`

### [ ] 24 — `ps`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `24_ps_frame_00.png` — missing | ⬜ `24_ps_frame_01.png` — missing |
| 02 | ⬜ `24_ps_frame_01.png` — missing | ⬜ `24_ps_frame_02.png` — missing |
| 03 | ⬜ `24_ps_frame_02.png` — missing | ⬜ `24_ps_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `24_ps_frame_00.png`
- [ ] `24_ps_frame_01.png`
- [ ] `24_ps_frame_02.png`
- [ ] `24_ps_frame_03.png`

### [ ] 25 — `grep`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `25_grep_frame_00.png` — missing | ⬜ `25_grep_frame_01.png` — missing |
| 02 | ⬜ `25_grep_frame_01.png` — missing | ⬜ `25_grep_frame_02.png` — missing |
| 03 | ⬜ `25_grep_frame_02.png` — missing | ⬜ `25_grep_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `25_grep_frame_00.png`
- [ ] `25_grep_frame_01.png`
- [ ] `25_grep_frame_02.png`
- [ ] `25_grep_frame_03.png`

### [ ] 26 — `ifconfig`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `26_ifconfig_frame_00.png` — missing | ⬜ `26_ifconfig_frame_01.png` — missing |
| 02 | ⬜ `26_ifconfig_frame_01.png` — missing | ⬜ `26_ifconfig_frame_02.png` — missing |
| 03 | ⬜ `26_ifconfig_frame_02.png` — missing | ⬜ `26_ifconfig_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `26_ifconfig_frame_00.png`
- [ ] `26_ifconfig_frame_01.png`
- [ ] `26_ifconfig_frame_02.png`
- [ ] `26_ifconfig_frame_03.png`

### [ ] 27 — `man`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `27_man_frame_00.png` — missing | ⬜ `27_man_frame_01.png` — missing |
| 02 | ⬜ `27_man_frame_01.png` — missing | ⬜ `27_man_frame_02.png` — missing |
| 03 | ⬜ `27_man_frame_02.png` — missing | ⬜ `27_man_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `27_man_frame_00.png`
- [ ] `27_man_frame_01.png`
- [ ] `27_man_frame_02.png`
- [ ] `27_man_frame_03.png`

### [ ] 28 — `history`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `28_history_frame_00.png` — missing | ⬜ `28_history_frame_01.png` — missing |
| 02 | ⬜ `28_history_frame_01.png` — missing | ⬜ `28_history_frame_02.png` — missing |
| 03 | ⬜ `28_history_frame_02.png` — missing | ⬜ `28_history_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `28_history_frame_00.png`
- [ ] `28_history_frame_01.png`
- [ ] `28_history_frame_02.png`
- [ ] `28_history_frame_03.png`

### [ ] 29 — `touch`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `29_touch_frame_00.png` — missing | ⬜ `29_touch_frame_01.png` — missing |
| 02 | ⬜ `29_touch_frame_01.png` — missing | ⬜ `29_touch_frame_02.png` — missing |
| 03 | ⬜ `29_touch_frame_02.png` — missing | ⬜ `29_touch_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `29_touch_frame_00.png`
- [ ] `29_touch_frame_01.png`
- [ ] `29_touch_frame_02.png`
- [ ] `29_touch_frame_03.png`

### [ ] 30 — `yes`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `30_yes_frame_00.png` — missing | ⬜ `30_yes_frame_01.png` — missing |
| 02 | ⬜ `30_yes_frame_01.png` — missing | ⬜ `30_yes_frame_02.png` — missing |
| 03 | ⬜ `30_yes_frame_02.png` — missing | ⬜ `30_yes_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `30_yes_frame_00.png`
- [ ] `30_yes_frame_01.png`
- [ ] `30_yes_frame_02.png`
- [ ] `30_yes_frame_03.png`

### [ ] 31 — `false`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `31_false_frame_00.png` — missing | ⬜ `31_false_frame_01.png` — missing |
| 02 | ⬜ `31_false_frame_01.png` — missing | ⬜ `31_false_frame_02.png` — missing |
| 03 | ⬜ `31_false_frame_02.png` — missing | ⬜ `31_false_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `31_false_frame_00.png`
- [ ] `31_false_frame_01.png`
- [ ] `31_false_frame_02.png`
- [ ] `31_false_frame_03.png`

### [ ] 32 — `|`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `32_pipe_frame_00.png` — missing | ⬜ `32_pipe_frame_01.png` — missing |
| 02 | ⬜ `32_pipe_frame_01.png` — missing | ⬜ `32_pipe_frame_02.png` — missing |
| 03 | ⬜ `32_pipe_frame_02.png` — missing | ⬜ `32_pipe_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `32_pipe_frame_00.png`
- [ ] `32_pipe_frame_01.png`
- [ ] `32_pipe_frame_02.png`
- [ ] `32_pipe_frame_03.png`

### [ ] 33 — `cp`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `33_cp_frame_00.png` — missing | ⬜ `33_cp_frame_01.png` — missing |
| 02 | ⬜ `33_cp_frame_01.png` — missing | ⬜ `33_cp_frame_02.png` — missing |
| 03 | ⬜ `33_cp_frame_02.png` — missing | ⬜ `33_cp_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `33_cp_frame_00.png`
- [ ] `33_cp_frame_01.png`
- [ ] `33_cp_frame_02.png`
- [ ] `33_cp_frame_03.png`

### [ ] 34 — `mv`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `34_mv_frame_00.png` — missing | ⬜ `34_mv_frame_01.png` — missing |
| 02 | ⬜ `34_mv_frame_01.png` — missing | ⬜ `34_mv_frame_02.png` — missing |
| 03 | ⬜ `34_mv_frame_02.png` — missing | ⬜ `34_mv_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `34_mv_frame_00.png`
- [ ] `34_mv_frame_01.png`
- [ ] `34_mv_frame_02.png`
- [ ] `34_mv_frame_03.png`

### [ ] 35 — `curl`

Unique canonical boundary frames: **0/4 present**. Adjacent clips intentionally share the same boundary image.

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `35_curl_frame_00.png` — missing | ⬜ `35_curl_frame_01.png` — missing |
| 02 | ⬜ `35_curl_frame_01.png` — missing | ⬜ `35_curl_frame_02.png` — missing |
| 03 | ⬜ `35_curl_frame_02.png` — missing | ⬜ `35_curl_frame_03.png` — missing |

**TODO — missing boundaries**

- [ ] `35_curl_frame_00.png`
- [ ] `35_curl_frame_01.png`
- [ ] `35_curl_frame_02.png`
- [ ] `35_curl_frame_03.png`

## Tangent animations

### [ ] 23_cat_theological_mechanical_history

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `not declared` — unresolved | ✅ `cat_01.png` |
| 02 | ✅ `cat_01.png` | ✅ `cat_02.png` |
| 03 | ✅ `cat_02.png` | ✅ `cat_03.png` |
| 04 | ✅ `cat_03.png` | ✅ `cat_04.png` |
| 05 | ✅ `cat_04.png` | ✅ `cat_05.png` |
| 06 | ✅ `cat_05.png` | ✅ `cat_06.png` |
| 07 | ✅ `cat_06.png` | ✅ `cat_07.png` |
| 08 | ✅ `cat_07.png` | ✅ `cat_08.png` |
| 09 | ✅ `cat_08.png` | ✅ `cat_09.png` |
| 10 | ✅ `cat_09.png` | ✅ `cat_10.png` |
| 11 | ✅ `cat_10.png` | ✅ `cat_11.png` |
| 12 | ✅ `cat_11.png` | ✅ `cat_12.png` |

**TODO — missing boundaries**

- [ ] scene 01 start boundary

### [ ] 34_mv_elevator_incident

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `not declared` — unresolved | ✅ `elevator_01.png` |
| 02 | ✅ `elevator_01.png` | ✅ `elevator_02.png` |
| 03 | ✅ `elevator_02.png` | ✅ `elevator_03.png` |
| 04 | ✅ `elevator_03.png` | ✅ `elevator_04.png` |
| 05 | ✅ `elevator_04.png` | ✅ `elevator_05.png` |
| 06 | ✅ `elevator_05.png` | ✅ `elevator_06.png` |
| 07 | ✅ `elevator_06.png` | ✅ `elevator_07.png` |
| 08 | ✅ `elevator_07.png` | ✅ `elevator_08.png` |
| 09 | ✅ `elevator_08.png` | ✅ `elevator_09.png` |
| 10 | ✅ `elevator_09.png` | ✅ `elevator_10.png` |
| 11 | ✅ `elevator_10.png` | ✅ `elevator_11.png` |
| 12 | ✅ `elevator_11.png` | ✅ `elevator_12.png` |

**TODO — missing boundaries**

- [ ] scene 01 start boundary

### [ ] 02_pkg_install_small_empire_of_dependencies

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `not declared` — unresolved | ✅ `package_01.png` |
| 02 | ✅ `package_01.png` | ✅ `package_02.png` |
| 03 | ✅ `package_02.png` | ✅ `package_03.png` |
| 04 | ✅ `package_03.png` | ✅ `package_04.png` |
| 05 | ✅ `package_04.png` | ✅ `package_05.png` |
| 06 | ✅ `package_05.png` | ✅ `package_06.png` |
| 07 | ✅ `package_06.png` | ✅ `package_07.png` |
| 08 | ✅ `package_07.png` | ✅ `package_08.png` |
| 09 | ✅ `package_08.png` | ✅ `package_09.png` |
| 10 | ✅ `package_09.png` | ✅ `package_10.png` |
| 11 | ✅ `package_10.png` | ✅ `package_11.png` |
| 12 | ✅ `package_11.png` | ✅ `package_12.png` |

**TODO — missing boundaries**

- [ ] scene 01 start boundary

### [ ] 22_find_cartographical_disaster

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ⬜ `not declared` — unresolved | ✅ `cartography_01.png` |
| 02 | ✅ `cartography_01.png` | ✅ `cartography_02.png` |
| 03 | ✅ `cartography_02.png` | ✅ `cartography_03.png` |
| 04 | ✅ `cartography_03.png` | ✅ `cartography_04.png` |
| 05 | ✅ `cartography_04.png` | ✅ `cartography_05.png` |
| 06 | ✅ `cartography_05.png` | ✅ `cartography_06.png` |

**TODO — missing boundaries**

- [ ] scene 01 start boundary

## Legacy / incomplete animations

### [x] legacy — `ls`

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ✅ `ls_clip_01_start.png` | ✅ `ls_clip_01_end.png` |
| 02 | ✅ `ls_clip_02_start.png` | ✅ `ls_clip_02_end.png` |
| 03 | ✅ `ls_clip_03_start.png` | ✅ `ls_clip_03_end.png` |
| 04 | ✅ `ls_clip_04_start.png` | ✅ `ls_clip_04_end.png` |

All start/end boundaries for this animation are materialized.

### [x] legacy — `echo`

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ✅ `echo_clip_01_start.png` | ✅ `echo_clip_01_end.png` |
| 02 | ✅ `echo_clip_02_start.png` | ✅ `echo_clip_02_end.png` |
| 03 | ✅ `echo_clip_03_start.png` | ✅ `echo_clip_03_end.png` |
| 04 | ✅ `echo_clip_04_start.png` | ✅ `echo_clip_04_end.png` |

All start/end boundaries for this animation are materialized.

### [x] legacy — `ls_tangent`

| Clip / scene | Start frame | End frame |
| ---: | --- | --- |
| 01 | ✅ `ls_tangent_clip_01_start.png` | ✅ `ls_tangent_clip_01_end.png` |
| 02 | ✅ `ls_tangent_clip_02_start.png` | ✅ `ls_tangent_clip_02_end.png` |
| 03 | ✅ `ls_tangent_clip_03_start.png` | ✅ `ls_tangent_clip_03_end.png` |
| 04 | ✅ `ls_tangent_clip_04_start.png` | ✅ `ls_tangent_clip_04_end.png` |
| 05 | ✅ `ls_tangent_clip_05_start.png` | ✅ `ls_tangent_clip_05_end.png` |
| 06 | ✅ `ls_tangent_clip_06_start.png` | ✅ `ls_tangent_clip_06_end.png` |

All start/end boundaries for this animation are materialized.
