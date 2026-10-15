# Animation shot inventory and missing boundaries

Source: `docs/guide/MANIFEST.json`. Generated review of the original cut-sheet declarations, existing source stills and per-clip START/END roles.

**Warning:** The 42 tangent START images are original screenshots with visible `Start:` labels, **not clean production frames**. All 42 tangent END frames are unverified.

| Group | Animations | Clips | Boundary positions | Source images | Absent/unresolved |
| --- | ---: | ---: | ---: | ---: | ---: |
| Canonical | 35 | 117 | 234 | 10 | 224 |
| Tangents | 4 | 42 | 84 | 42 | 42 |
| Legacy | 3 | 14 | 28 | 28 | 0 |
| **Total** | **42** | **173** | **346** | **80** | **266** |

Canonical unique named frame files: **6/152**. Canonical neighboring cuts share the identical bytes for common boundaries.

[Individual clip scripts, shot descriptions and prompts](CLIP_PRODUCTION_INDEX.md)

## Required outstanding images

Do not mark an item complete without inspecting the actual asset. A file name alone or a numbered tangent screenshot is not proof of an END shot.

## Canonical

### pwd — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/01_pwd/clips/01/README.md) | ⬜ `01_pwd_frame_00.png` — missing | ⬜ `01_pwd_frame_01.png` — missing |
| [02](animations/canonical/01_pwd/clips/02/README.md) | ⬜ `01_pwd_frame_01.png` — missing | ⬜ `01_pwd_frame_02.png` — missing |
| [03](animations/canonical/01_pwd/clips/03/README.md) | ⬜ `01_pwd_frame_02.png` — missing | ⬜ `01_pwd_frame_03.png` — missing |
| [04](animations/canonical/01_pwd/clips/04/README.md) | ⬜ `01_pwd_frame_03.png` — missing | ⬜ `01_pwd_frame_04.png` — missing |

- [ ] Clip 01 START: 01_pwd_frame_00.png
- [ ] Clip 01 END: 01_pwd_frame_01.png
- [ ] Clip 02 START: 01_pwd_frame_01.png
- [ ] Clip 02 END: 01_pwd_frame_02.png
- [ ] Clip 03 START: 01_pwd_frame_02.png
- [ ] Clip 03 END: 01_pwd_frame_03.png
- [ ] Clip 04 START: 01_pwd_frame_03.png
- [ ] Clip 04 END: 01_pwd_frame_04.png

### pkg install — both boundaries supplied

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/02_pkg_install/clips/01/README.md) | ✅ `02_pkg_install_frame_00.png` | ✅ `02_pkg_install_frame_01.png` |
| [02](animations/canonical/02_pkg_install/clips/02/README.md) | ✅ `02_pkg_install_frame_01.png` | ✅ `02_pkg_install_frame_02.png` |
| [03](animations/canonical/02_pkg_install/clips/03/README.md) | ✅ `02_pkg_install_frame_02.png` | ✅ `02_pkg_install_frame_03.png` |
| [04](animations/canonical/02_pkg_install/clips/04/README.md) | ✅ `02_pkg_install_frame_03.png` | ✅ `02_pkg_install_frame_04.png` |

### apt-get update — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/03_apt_get_update/clips/01/README.md) | ⬜ `03_apt_get_update_frame_00.png` — missing | ✅ `03_apt_get_update_frame_01.png` |
| [02](animations/canonical/03_apt_get_update/clips/02/README.md) | ✅ `03_apt_get_update_frame_01.png` | ⬜ `03_apt_get_update_frame_02.png` — missing |
| [03](animations/canonical/03_apt_get_update/clips/03/README.md) | ⬜ `03_apt_get_update_frame_02.png` — missing | ⬜ `03_apt_get_update_frame_03.png` — missing |

- [ ] Clip 01 START: 03_apt_get_update_frame_00.png
- [ ] Clip 02 END: 03_apt_get_update_frame_02.png
- [ ] Clip 03 START: 03_apt_get_update_frame_02.png
- [ ] Clip 03 END: 03_apt_get_update_frame_03.png

### nano — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/04_nano/clips/01/README.md) | ⬜ `04_nano_frame_00.png` — missing | ⬜ `04_nano_frame_01.png` — missing |
| [02](animations/canonical/04_nano/clips/02/README.md) | ⬜ `04_nano_frame_01.png` — missing | ⬜ `04_nano_frame_02.png` — missing |
| [03](animations/canonical/04_nano/clips/03/README.md) | ⬜ `04_nano_frame_02.png` — missing | ⬜ `04_nano_frame_03.png` — missing |

- [ ] Clip 01 START: 04_nano_frame_00.png
- [ ] Clip 01 END: 04_nano_frame_01.png
- [ ] Clip 02 START: 04_nano_frame_01.png
- [ ] Clip 02 END: 04_nano_frame_02.png
- [ ] Clip 03 START: 04_nano_frame_02.png
- [ ] Clip 03 END: 04_nano_frame_03.png

### top — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/05_top/clips/01/README.md) | ⬜ `05_top_frame_00.png` — missing | ⬜ `05_top_frame_01.png` — missing |
| [02](animations/canonical/05_top/clips/02/README.md) | ⬜ `05_top_frame_01.png` — missing | ⬜ `05_top_frame_02.png` — missing |
| [03](animations/canonical/05_top/clips/03/README.md) | ⬜ `05_top_frame_02.png` — missing | ⬜ `05_top_frame_03.png` — missing |

- [ ] Clip 01 START: 05_top_frame_00.png
- [ ] Clip 01 END: 05_top_frame_01.png
- [ ] Clip 02 START: 05_top_frame_01.png
- [ ] Clip 02 END: 05_top_frame_02.png
- [ ] Clip 03 START: 05_top_frame_02.png
- [ ] Clip 03 END: 05_top_frame_03.png

### kill — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/06_kill/clips/01/README.md) | ⬜ `06_kill_frame_00.png` — missing | ⬜ `06_kill_frame_01.png` — missing |
| [02](animations/canonical/06_kill/clips/02/README.md) | ⬜ `06_kill_frame_01.png` — missing | ⬜ `06_kill_frame_02.png` — missing |
| [03](animations/canonical/06_kill/clips/03/README.md) | ⬜ `06_kill_frame_02.png` — missing | ⬜ `06_kill_frame_03.png` — missing |

- [ ] Clip 01 START: 06_kill_frame_00.png
- [ ] Clip 01 END: 06_kill_frame_01.png
- [ ] Clip 02 START: 06_kill_frame_01.png
- [ ] Clip 02 END: 06_kill_frame_02.png
- [ ] Clip 03 START: 06_kill_frame_02.png
- [ ] Clip 03 END: 06_kill_frame_03.png

### df — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/07_df/clips/01/README.md) | ⬜ `07_df_frame_00.png` — missing | ⬜ `07_df_frame_01.png` — missing |
| [02](animations/canonical/07_df/clips/02/README.md) | ⬜ `07_df_frame_01.png` — missing | ⬜ `07_df_frame_02.png` — missing |
| [03](animations/canonical/07_df/clips/03/README.md) | ⬜ `07_df_frame_02.png` — missing | ⬜ `07_df_frame_03.png` — missing |

- [ ] Clip 01 START: 07_df_frame_00.png
- [ ] Clip 01 END: 07_df_frame_01.png
- [ ] Clip 02 START: 07_df_frame_01.png
- [ ] Clip 02 END: 07_df_frame_02.png
- [ ] Clip 03 START: 07_df_frame_02.png
- [ ] Clip 03 END: 07_df_frame_03.png

### rm — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/08_rm/clips/01/README.md) | ⬜ `08_rm_frame_00.png` — missing | ⬜ `08_rm_frame_01.png` — missing |
| [02](animations/canonical/08_rm/clips/02/README.md) | ⬜ `08_rm_frame_01.png` — missing | ⬜ `08_rm_frame_02.png` — missing |
| [03](animations/canonical/08_rm/clips/03/README.md) | ⬜ `08_rm_frame_02.png` — missing | ⬜ `08_rm_frame_03.png` — missing |
| [04](animations/canonical/08_rm/clips/04/README.md) | ⬜ `08_rm_frame_03.png` — missing | ⬜ `08_rm_frame_04.png` — missing |

- [ ] Clip 01 START: 08_rm_frame_00.png
- [ ] Clip 01 END: 08_rm_frame_01.png
- [ ] Clip 02 START: 08_rm_frame_01.png
- [ ] Clip 02 END: 08_rm_frame_02.png
- [ ] Clip 03 START: 08_rm_frame_02.png
- [ ] Clip 03 END: 08_rm_frame_03.png
- [ ] Clip 04 START: 08_rm_frame_03.png
- [ ] Clip 04 END: 08_rm_frame_04.png

### source — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/09_source/clips/01/README.md) | ⬜ `09_source_frame_00.png` — missing | ⬜ `09_source_frame_01.png` — missing |
| [02](animations/canonical/09_source/clips/02/README.md) | ⬜ `09_source_frame_01.png` — missing | ⬜ `09_source_frame_02.png` — missing |
| [03](animations/canonical/09_source/clips/03/README.md) | ⬜ `09_source_frame_02.png` — missing | ⬜ `09_source_frame_03.png` — missing |
| [04](animations/canonical/09_source/clips/04/README.md) | ⬜ `09_source_frame_03.png` — missing | ⬜ `09_source_frame_04.png` — missing |

- [ ] Clip 01 START: 09_source_frame_00.png
- [ ] Clip 01 END: 09_source_frame_01.png
- [ ] Clip 02 START: 09_source_frame_01.png
- [ ] Clip 02 END: 09_source_frame_02.png
- [ ] Clip 03 START: 09_source_frame_02.png
- [ ] Clip 03 END: 09_source_frame_03.png
- [ ] Clip 04 START: 09_source_frame_03.png
- [ ] Clip 04 END: 09_source_frame_04.png

### ping — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/10_ping/clips/01/README.md) | ⬜ `10_ping_frame_00.png` — missing | ⬜ `10_ping_frame_01.png` — missing |
| [02](animations/canonical/10_ping/clips/02/README.md) | ⬜ `10_ping_frame_01.png` — missing | ⬜ `10_ping_frame_02.png` — missing |
| [03](animations/canonical/10_ping/clips/03/README.md) | ⬜ `10_ping_frame_02.png` — missing | ⬜ `10_ping_frame_03.png` — missing |
| [04](animations/canonical/10_ping/clips/04/README.md) | ⬜ `10_ping_frame_03.png` — missing | ⬜ `10_ping_frame_04.png` — missing |

- [ ] Clip 01 START: 10_ping_frame_00.png
- [ ] Clip 01 END: 10_ping_frame_01.png
- [ ] Clip 02 START: 10_ping_frame_01.png
- [ ] Clip 02 END: 10_ping_frame_02.png
- [ ] Clip 03 START: 10_ping_frame_02.png
- [ ] Clip 03 END: 10_ping_frame_03.png
- [ ] Clip 04 START: 10_ping_frame_03.png
- [ ] Clip 04 END: 10_ping_frame_04.png

### ssh — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/11_ssh/clips/01/README.md) | ⬜ `11_ssh_frame_00.png` — missing | ⬜ `11_ssh_frame_01.png` — missing |
| [02](animations/canonical/11_ssh/clips/02/README.md) | ⬜ `11_ssh_frame_01.png` — missing | ⬜ `11_ssh_frame_02.png` — missing |
| [03](animations/canonical/11_ssh/clips/03/README.md) | ⬜ `11_ssh_frame_02.png` — missing | ⬜ `11_ssh_frame_03.png` — missing |
| [04](animations/canonical/11_ssh/clips/04/README.md) | ⬜ `11_ssh_frame_03.png` — missing | ⬜ `11_ssh_frame_04.png` — missing |

- [ ] Clip 01 START: 11_ssh_frame_00.png
- [ ] Clip 01 END: 11_ssh_frame_01.png
- [ ] Clip 02 START: 11_ssh_frame_01.png
- [ ] Clip 02 END: 11_ssh_frame_02.png
- [ ] Clip 03 START: 11_ssh_frame_02.png
- [ ] Clip 03 END: 11_ssh_frame_03.png
- [ ] Clip 04 START: 11_ssh_frame_03.png
- [ ] Clip 04 END: 11_ssh_frame_04.png

### harden-check — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/12_harden_check/clips/01/README.md) | ⬜ `12_harden_check_frame_00.png` — missing | ⬜ `12_harden_check_frame_01.png` — missing |
| [02](animations/canonical/12_harden_check/clips/02/README.md) | ⬜ `12_harden_check_frame_01.png` — missing | ⬜ `12_harden_check_frame_02.png` — missing |
| [03](animations/canonical/12_harden_check/clips/03/README.md) | ⬜ `12_harden_check_frame_02.png` — missing | ⬜ `12_harden_check_frame_03.png` — missing |

- [ ] Clip 01 START: 12_harden_check_frame_00.png
- [ ] Clip 01 END: 12_harden_check_frame_01.png
- [ ] Clip 02 START: 12_harden_check_frame_01.png
- [ ] Clip 02 END: 12_harden_check_frame_02.png
- [ ] Clip 03 START: 12_harden_check_frame_02.png
- [ ] Clip 03 END: 12_harden_check_frame_03.png

### git commit — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/13_git_commit/clips/01/README.md) | ⬜ `13_git_commit_frame_00.png` — missing | ⬜ `13_git_commit_frame_01.png` — missing |
| [02](animations/canonical/13_git_commit/clips/02/README.md) | ⬜ `13_git_commit_frame_01.png` — missing | ⬜ `13_git_commit_frame_02.png` — missing |
| [03](animations/canonical/13_git_commit/clips/03/README.md) | ⬜ `13_git_commit_frame_02.png` — missing | ⬜ `13_git_commit_frame_03.png` — missing |

- [ ] Clip 01 START: 13_git_commit_frame_00.png
- [ ] Clip 01 END: 13_git_commit_frame_01.png
- [ ] Clip 02 START: 13_git_commit_frame_01.png
- [ ] Clip 02 END: 13_git_commit_frame_02.png
- [ ] Clip 03 START: 13_git_commit_frame_02.png
- [ ] Clip 03 END: 13_git_commit_frame_03.png

### nmap — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/14_nmap/clips/01/README.md) | ⬜ `14_nmap_frame_00.png` — missing | ⬜ `14_nmap_frame_01.png` — missing |
| [02](animations/canonical/14_nmap/clips/02/README.md) | ⬜ `14_nmap_frame_01.png` — missing | ⬜ `14_nmap_frame_02.png` — missing |
| [03](animations/canonical/14_nmap/clips/03/README.md) | ⬜ `14_nmap_frame_02.png` — missing | ⬜ `14_nmap_frame_03.png` — missing |

- [ ] Clip 01 START: 14_nmap_frame_00.png
- [ ] Clip 01 END: 14_nmap_frame_01.png
- [ ] Clip 02 START: 14_nmap_frame_01.png
- [ ] Clip 02 END: 14_nmap_frame_02.png
- [ ] Clip 03 START: 14_nmap_frame_02.png
- [ ] Clip 03 END: 14_nmap_frame_03.png

### crond — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/15_crond/clips/01/README.md) | ⬜ `15_crond_frame_00.png` — missing | ⬜ `15_crond_frame_01.png` — missing |
| [02](animations/canonical/15_crond/clips/02/README.md) | ⬜ `15_crond_frame_01.png` — missing | ⬜ `15_crond_frame_02.png` — missing |
| [03](animations/canonical/15_crond/clips/03/README.md) | ⬜ `15_crond_frame_02.png` — missing | ⬜ `15_crond_frame_03.png` — missing |
| [04](animations/canonical/15_crond/clips/04/README.md) | ⬜ `15_crond_frame_03.png` — missing | ⬜ `15_crond_frame_04.png` — missing |

- [ ] Clip 01 START: 15_crond_frame_00.png
- [ ] Clip 01 END: 15_crond_frame_01.png
- [ ] Clip 02 START: 15_crond_frame_01.png
- [ ] Clip 02 END: 15_crond_frame_02.png
- [ ] Clip 03 START: 15_crond_frame_02.png
- [ ] Clip 03 END: 15_crond_frame_03.png
- [ ] Clip 04 START: 15_crond_frame_03.png
- [ ] Clip 04 END: 15_crond_frame_04.png

### watch — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/16_watch/clips/01/README.md) | ⬜ `16_watch_frame_00.png` — missing | ⬜ `16_watch_frame_01.png` — missing |
| [02](animations/canonical/16_watch/clips/02/README.md) | ⬜ `16_watch_frame_01.png` — missing | ⬜ `16_watch_frame_02.png` — missing |
| [03](animations/canonical/16_watch/clips/03/README.md) | ⬜ `16_watch_frame_02.png` — missing | ⬜ `16_watch_frame_03.png` — missing |

- [ ] Clip 01 START: 16_watch_frame_00.png
- [ ] Clip 01 END: 16_watch_frame_01.png
- [ ] Clip 02 START: 16_watch_frame_01.png
- [ ] Clip 02 END: 16_watch_frame_02.png
- [ ] Clip 03 START: 16_watch_frame_02.png
- [ ] Clip 03 END: 16_watch_frame_03.png

### AI chat — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/17_ai_chat/clips/01/README.md) | ⬜ `17_ai_chat_frame_00.png` — missing | ⬜ `17_ai_chat_frame_01.png` — missing |
| [02](animations/canonical/17_ai_chat/clips/02/README.md) | ⬜ `17_ai_chat_frame_01.png` — missing | ⬜ `17_ai_chat_frame_02.png` — missing |
| [03](animations/canonical/17_ai_chat/clips/03/README.md) | ⬜ `17_ai_chat_frame_02.png` — missing | ⬜ `17_ai_chat_frame_03.png` — missing |
| [04](animations/canonical/17_ai_chat/clips/04/README.md) | ⬜ `17_ai_chat_frame_03.png` — missing | ⬜ `17_ai_chat_frame_04.png` — missing |

- [ ] Clip 01 START: 17_ai_chat_frame_00.png
- [ ] Clip 01 END: 17_ai_chat_frame_01.png
- [ ] Clip 02 START: 17_ai_chat_frame_01.png
- [ ] Clip 02 END: 17_ai_chat_frame_02.png
- [ ] Clip 03 START: 17_ai_chat_frame_02.png
- [ ] Clip 03 END: 17_ai_chat_frame_03.png
- [ ] Clip 04 START: 17_ai_chat_frame_03.png
- [ ] Clip 04 END: 17_ai_chat_frame_04.png

### skill — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/18_skill/clips/01/README.md) | ⬜ `18_skill_frame_00.png` — missing | ⬜ `18_skill_frame_01.png` — missing |
| [02](animations/canonical/18_skill/clips/02/README.md) | ⬜ `18_skill_frame_01.png` — missing | ⬜ `18_skill_frame_02.png` — missing |
| [03](animations/canonical/18_skill/clips/03/README.md) | ⬜ `18_skill_frame_02.png` — missing | ⬜ `18_skill_frame_03.png` — missing |
| [04](animations/canonical/18_skill/clips/04/README.md) | ⬜ `18_skill_frame_03.png` — missing | ⬜ `18_skill_frame_04.png` — missing |

- [ ] Clip 01 START: 18_skill_frame_00.png
- [ ] Clip 01 END: 18_skill_frame_01.png
- [ ] Clip 02 START: 18_skill_frame_01.png
- [ ] Clip 02 END: 18_skill_frame_02.png
- [ ] Clip 03 START: 18_skill_frame_02.png
- [ ] Clip 03 END: 18_skill_frame_03.png
- [ ] Clip 04 START: 18_skill_frame_03.png
- [ ] Clip 04 END: 18_skill_frame_04.png

### ls -a — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/19_ls_a/clips/01/README.md) | ⬜ `19_ls_a_frame_00.png` — missing | ⬜ `19_ls_a_frame_01.png` — missing |
| [02](animations/canonical/19_ls_a/clips/02/README.md) | ⬜ `19_ls_a_frame_01.png` — missing | ⬜ `19_ls_a_frame_02.png` — missing |
| [03](animations/canonical/19_ls_a/clips/03/README.md) | ⬜ `19_ls_a_frame_02.png` — missing | ⬜ `19_ls_a_frame_03.png` — missing |
| [04](animations/canonical/19_ls_a/clips/04/README.md) | ⬜ `19_ls_a_frame_03.png` — missing | ⬜ `19_ls_a_frame_04.png` — missing |

- [ ] Clip 01 START: 19_ls_a_frame_00.png
- [ ] Clip 01 END: 19_ls_a_frame_01.png
- [ ] Clip 02 START: 19_ls_a_frame_01.png
- [ ] Clip 02 END: 19_ls_a_frame_02.png
- [ ] Clip 03 START: 19_ls_a_frame_02.png
- [ ] Clip 03 END: 19_ls_a_frame_03.png
- [ ] Clip 04 START: 19_ls_a_frame_03.png
- [ ] Clip 04 END: 19_ls_a_frame_04.png

### sudo — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/20_sudo/clips/01/README.md) | ⬜ `20_sudo_frame_00.png` — missing | ⬜ `20_sudo_frame_01.png` — missing |
| [02](animations/canonical/20_sudo/clips/02/README.md) | ⬜ `20_sudo_frame_01.png` — missing | ⬜ `20_sudo_frame_02.png` — missing |
| [03](animations/canonical/20_sudo/clips/03/README.md) | ⬜ `20_sudo_frame_02.png` — missing | ⬜ `20_sudo_frame_03.png` — missing |

- [ ] Clip 01 START: 20_sudo_frame_00.png
- [ ] Clip 01 END: 20_sudo_frame_01.png
- [ ] Clip 02 START: 20_sudo_frame_01.png
- [ ] Clip 02 END: 20_sudo_frame_02.png
- [ ] Clip 03 START: 20_sudo_frame_02.png
- [ ] Clip 03 END: 20_sudo_frame_03.png

### rmdir — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/21_rmdir/clips/01/README.md) | ⬜ `21_rmdir_frame_00.png` — missing | ⬜ `21_rmdir_frame_01.png` — missing |
| [02](animations/canonical/21_rmdir/clips/02/README.md) | ⬜ `21_rmdir_frame_01.png` — missing | ⬜ `21_rmdir_frame_02.png` — missing |
| [03](animations/canonical/21_rmdir/clips/03/README.md) | ⬜ `21_rmdir_frame_02.png` — missing | ⬜ `21_rmdir_frame_03.png` — missing |
| [04](animations/canonical/21_rmdir/clips/04/README.md) | ⬜ `21_rmdir_frame_03.png` — missing | ⬜ `21_rmdir_frame_04.png` — missing |

- [ ] Clip 01 START: 21_rmdir_frame_00.png
- [ ] Clip 01 END: 21_rmdir_frame_01.png
- [ ] Clip 02 START: 21_rmdir_frame_01.png
- [ ] Clip 02 END: 21_rmdir_frame_02.png
- [ ] Clip 03 START: 21_rmdir_frame_02.png
- [ ] Clip 03 END: 21_rmdir_frame_03.png
- [ ] Clip 04 START: 21_rmdir_frame_03.png
- [ ] Clip 04 END: 21_rmdir_frame_04.png

### find — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/22_find/clips/01/README.md) | ⬜ `22_find_frame_00.png` — missing | ⬜ `22_find_frame_01.png` — missing |
| [02](animations/canonical/22_find/clips/02/README.md) | ⬜ `22_find_frame_01.png` — missing | ⬜ `22_find_frame_02.png` — missing |
| [03](animations/canonical/22_find/clips/03/README.md) | ⬜ `22_find_frame_02.png` — missing | ⬜ `22_find_frame_03.png` — missing |
| [04](animations/canonical/22_find/clips/04/README.md) | ⬜ `22_find_frame_03.png` — missing | ⬜ `22_find_frame_04.png` — missing |

- [ ] Clip 01 START: 22_find_frame_00.png
- [ ] Clip 01 END: 22_find_frame_01.png
- [ ] Clip 02 START: 22_find_frame_01.png
- [ ] Clip 02 END: 22_find_frame_02.png
- [ ] Clip 03 START: 22_find_frame_02.png
- [ ] Clip 03 END: 22_find_frame_03.png
- [ ] Clip 04 START: 22_find_frame_03.png
- [ ] Clip 04 END: 22_find_frame_04.png

### cat — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/23_cat/clips/01/README.md) | ⬜ `23_cat_frame_00.png` — missing | ⬜ `23_cat_frame_01.png` — missing |
| [02](animations/canonical/23_cat/clips/02/README.md) | ⬜ `23_cat_frame_01.png` — missing | ⬜ `23_cat_frame_02.png` — missing |
| [03](animations/canonical/23_cat/clips/03/README.md) | ⬜ `23_cat_frame_02.png` — missing | ⬜ `23_cat_frame_03.png` — missing |

- [ ] Clip 01 START: 23_cat_frame_00.png
- [ ] Clip 01 END: 23_cat_frame_01.png
- [ ] Clip 02 START: 23_cat_frame_01.png
- [ ] Clip 02 END: 23_cat_frame_02.png
- [ ] Clip 03 START: 23_cat_frame_02.png
- [ ] Clip 03 END: 23_cat_frame_03.png

### ps — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/24_ps/clips/01/README.md) | ⬜ `24_ps_frame_00.png` — missing | ⬜ `24_ps_frame_01.png` — missing |
| [02](animations/canonical/24_ps/clips/02/README.md) | ⬜ `24_ps_frame_01.png` — missing | ⬜ `24_ps_frame_02.png` — missing |
| [03](animations/canonical/24_ps/clips/03/README.md) | ⬜ `24_ps_frame_02.png` — missing | ⬜ `24_ps_frame_03.png` — missing |

- [ ] Clip 01 START: 24_ps_frame_00.png
- [ ] Clip 01 END: 24_ps_frame_01.png
- [ ] Clip 02 START: 24_ps_frame_01.png
- [ ] Clip 02 END: 24_ps_frame_02.png
- [ ] Clip 03 START: 24_ps_frame_02.png
- [ ] Clip 03 END: 24_ps_frame_03.png

### grep — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/25_grep/clips/01/README.md) | ⬜ `25_grep_frame_00.png` — missing | ⬜ `25_grep_frame_01.png` — missing |
| [02](animations/canonical/25_grep/clips/02/README.md) | ⬜ `25_grep_frame_01.png` — missing | ⬜ `25_grep_frame_02.png` — missing |
| [03](animations/canonical/25_grep/clips/03/README.md) | ⬜ `25_grep_frame_02.png` — missing | ⬜ `25_grep_frame_03.png` — missing |

- [ ] Clip 01 START: 25_grep_frame_00.png
- [ ] Clip 01 END: 25_grep_frame_01.png
- [ ] Clip 02 START: 25_grep_frame_01.png
- [ ] Clip 02 END: 25_grep_frame_02.png
- [ ] Clip 03 START: 25_grep_frame_02.png
- [ ] Clip 03 END: 25_grep_frame_03.png

### ifconfig — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/26_ifconfig/clips/01/README.md) | ⬜ `26_ifconfig_frame_00.png` — missing | ⬜ `26_ifconfig_frame_01.png` — missing |
| [02](animations/canonical/26_ifconfig/clips/02/README.md) | ⬜ `26_ifconfig_frame_01.png` — missing | ⬜ `26_ifconfig_frame_02.png` — missing |
| [03](animations/canonical/26_ifconfig/clips/03/README.md) | ⬜ `26_ifconfig_frame_02.png` — missing | ⬜ `26_ifconfig_frame_03.png` — missing |

- [ ] Clip 01 START: 26_ifconfig_frame_00.png
- [ ] Clip 01 END: 26_ifconfig_frame_01.png
- [ ] Clip 02 START: 26_ifconfig_frame_01.png
- [ ] Clip 02 END: 26_ifconfig_frame_02.png
- [ ] Clip 03 START: 26_ifconfig_frame_02.png
- [ ] Clip 03 END: 26_ifconfig_frame_03.png

### man — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/27_man/clips/01/README.md) | ⬜ `27_man_frame_00.png` — missing | ⬜ `27_man_frame_01.png` — missing |
| [02](animations/canonical/27_man/clips/02/README.md) | ⬜ `27_man_frame_01.png` — missing | ⬜ `27_man_frame_02.png` — missing |
| [03](animations/canonical/27_man/clips/03/README.md) | ⬜ `27_man_frame_02.png` — missing | ⬜ `27_man_frame_03.png` — missing |

- [ ] Clip 01 START: 27_man_frame_00.png
- [ ] Clip 01 END: 27_man_frame_01.png
- [ ] Clip 02 START: 27_man_frame_01.png
- [ ] Clip 02 END: 27_man_frame_02.png
- [ ] Clip 03 START: 27_man_frame_02.png
- [ ] Clip 03 END: 27_man_frame_03.png

### history — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/28_history/clips/01/README.md) | ⬜ `28_history_frame_00.png` — missing | ⬜ `28_history_frame_01.png` — missing |
| [02](animations/canonical/28_history/clips/02/README.md) | ⬜ `28_history_frame_01.png` — missing | ⬜ `28_history_frame_02.png` — missing |
| [03](animations/canonical/28_history/clips/03/README.md) | ⬜ `28_history_frame_02.png` — missing | ⬜ `28_history_frame_03.png` — missing |

- [ ] Clip 01 START: 28_history_frame_00.png
- [ ] Clip 01 END: 28_history_frame_01.png
- [ ] Clip 02 START: 28_history_frame_01.png
- [ ] Clip 02 END: 28_history_frame_02.png
- [ ] Clip 03 START: 28_history_frame_02.png
- [ ] Clip 03 END: 28_history_frame_03.png

### touch — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/29_touch/clips/01/README.md) | ⬜ `29_touch_frame_00.png` — missing | ⬜ `29_touch_frame_01.png` — missing |
| [02](animations/canonical/29_touch/clips/02/README.md) | ⬜ `29_touch_frame_01.png` — missing | ⬜ `29_touch_frame_02.png` — missing |
| [03](animations/canonical/29_touch/clips/03/README.md) | ⬜ `29_touch_frame_02.png` — missing | ⬜ `29_touch_frame_03.png` — missing |

- [ ] Clip 01 START: 29_touch_frame_00.png
- [ ] Clip 01 END: 29_touch_frame_01.png
- [ ] Clip 02 START: 29_touch_frame_01.png
- [ ] Clip 02 END: 29_touch_frame_02.png
- [ ] Clip 03 START: 29_touch_frame_02.png
- [ ] Clip 03 END: 29_touch_frame_03.png

### yes — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/30_yes/clips/01/README.md) | ⬜ `30_yes_frame_00.png` — missing | ⬜ `30_yes_frame_01.png` — missing |
| [02](animations/canonical/30_yes/clips/02/README.md) | ⬜ `30_yes_frame_01.png` — missing | ⬜ `30_yes_frame_02.png` — missing |
| [03](animations/canonical/30_yes/clips/03/README.md) | ⬜ `30_yes_frame_02.png` — missing | ⬜ `30_yes_frame_03.png` — missing |

- [ ] Clip 01 START: 30_yes_frame_00.png
- [ ] Clip 01 END: 30_yes_frame_01.png
- [ ] Clip 02 START: 30_yes_frame_01.png
- [ ] Clip 02 END: 30_yes_frame_02.png
- [ ] Clip 03 START: 30_yes_frame_02.png
- [ ] Clip 03 END: 30_yes_frame_03.png

### false — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/31_false/clips/01/README.md) | ⬜ `31_false_frame_00.png` — missing | ⬜ `31_false_frame_01.png` — missing |
| [02](animations/canonical/31_false/clips/02/README.md) | ⬜ `31_false_frame_01.png` — missing | ⬜ `31_false_frame_02.png` — missing |
| [03](animations/canonical/31_false/clips/03/README.md) | ⬜ `31_false_frame_02.png` — missing | ⬜ `31_false_frame_03.png` — missing |

- [ ] Clip 01 START: 31_false_frame_00.png
- [ ] Clip 01 END: 31_false_frame_01.png
- [ ] Clip 02 START: 31_false_frame_01.png
- [ ] Clip 02 END: 31_false_frame_02.png
- [ ] Clip 03 START: 31_false_frame_02.png
- [ ] Clip 03 END: 31_false_frame_03.png

### | — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/32_pipe/clips/01/README.md) | ⬜ `32_pipe_frame_00.png` — missing | ⬜ `32_pipe_frame_01.png` — missing |
| [02](animations/canonical/32_pipe/clips/02/README.md) | ⬜ `32_pipe_frame_01.png` — missing | ⬜ `32_pipe_frame_02.png` — missing |
| [03](animations/canonical/32_pipe/clips/03/README.md) | ⬜ `32_pipe_frame_02.png` — missing | ⬜ `32_pipe_frame_03.png` — missing |

- [ ] Clip 01 START: 32_pipe_frame_00.png
- [ ] Clip 01 END: 32_pipe_frame_01.png
- [ ] Clip 02 START: 32_pipe_frame_01.png
- [ ] Clip 02 END: 32_pipe_frame_02.png
- [ ] Clip 03 START: 32_pipe_frame_02.png
- [ ] Clip 03 END: 32_pipe_frame_03.png

### cp — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/33_cp/clips/01/README.md) | ⬜ `33_cp_frame_00.png` — missing | ⬜ `33_cp_frame_01.png` — missing |
| [02](animations/canonical/33_cp/clips/02/README.md) | ⬜ `33_cp_frame_01.png` — missing | ⬜ `33_cp_frame_02.png` — missing |
| [03](animations/canonical/33_cp/clips/03/README.md) | ⬜ `33_cp_frame_02.png` — missing | ⬜ `33_cp_frame_03.png` — missing |

- [ ] Clip 01 START: 33_cp_frame_00.png
- [ ] Clip 01 END: 33_cp_frame_01.png
- [ ] Clip 02 START: 33_cp_frame_01.png
- [ ] Clip 02 END: 33_cp_frame_02.png
- [ ] Clip 03 START: 33_cp_frame_02.png
- [ ] Clip 03 END: 33_cp_frame_03.png

### mv — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/34_mv/clips/01/README.md) | ⬜ `34_mv_frame_00.png` — missing | ⬜ `34_mv_frame_01.png` — missing |
| [02](animations/canonical/34_mv/clips/02/README.md) | ⬜ `34_mv_frame_01.png` — missing | ⬜ `34_mv_frame_02.png` — missing |
| [03](animations/canonical/34_mv/clips/03/README.md) | ⬜ `34_mv_frame_02.png` — missing | ⬜ `34_mv_frame_03.png` — missing |

- [ ] Clip 01 START: 34_mv_frame_00.png
- [ ] Clip 01 END: 34_mv_frame_01.png
- [ ] Clip 02 START: 34_mv_frame_01.png
- [ ] Clip 02 END: 34_mv_frame_02.png
- [ ] Clip 03 START: 34_mv_frame_02.png
- [ ] Clip 03 END: 34_mv_frame_03.png

### curl — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/canonical/35_curl/clips/01/README.md) | ⬜ `35_curl_frame_00.png` — missing | ⬜ `35_curl_frame_01.png` — missing |
| [02](animations/canonical/35_curl/clips/02/README.md) | ⬜ `35_curl_frame_01.png` — missing | ⬜ `35_curl_frame_02.png` — missing |
| [03](animations/canonical/35_curl/clips/03/README.md) | ⬜ `35_curl_frame_02.png` — missing | ⬜ `35_curl_frame_03.png` — missing |

- [ ] Clip 01 START: 35_curl_frame_00.png
- [ ] Clip 01 END: 35_curl_frame_01.png
- [ ] Clip 02 START: 35_curl_frame_01.png
- [ ] Clip 02 END: 35_curl_frame_02.png
- [ ] Clip 03 START: 35_curl_frame_02.png
- [ ] Clip 03 END: 35_curl_frame_03.png

## Tangents

### 23_cat_theological_mechanical_history — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/tangents/23_cat_theological_mechanical_history/clips/01/README.md) | ✅ `cat_01.png` (annotated) | ⬜ not supplied — unresolved |
| [02](animations/tangents/23_cat_theological_mechanical_history/clips/02/README.md) | ✅ `cat_02.png` (annotated) | ⬜ not supplied — unresolved |
| [03](animations/tangents/23_cat_theological_mechanical_history/clips/03/README.md) | ✅ `cat_03.png` (annotated) | ⬜ not supplied — unresolved |
| [04](animations/tangents/23_cat_theological_mechanical_history/clips/04/README.md) | ✅ `cat_04.png` (annotated) | ⬜ not supplied — unresolved |
| [05](animations/tangents/23_cat_theological_mechanical_history/clips/05/README.md) | ✅ `cat_05.png` (annotated) | ⬜ not supplied — unresolved |
| [06](animations/tangents/23_cat_theological_mechanical_history/clips/06/README.md) | ✅ `cat_06.png` (annotated) | ⬜ not supplied — unresolved |
| [07](animations/tangents/23_cat_theological_mechanical_history/clips/07/README.md) | ✅ `cat_07.png` (annotated) | ⬜ not supplied — unresolved |
| [08](animations/tangents/23_cat_theological_mechanical_history/clips/08/README.md) | ✅ `cat_08.png` (annotated) | ⬜ not supplied — unresolved |
| [09](animations/tangents/23_cat_theological_mechanical_history/clips/09/README.md) | ✅ `cat_09.png` (annotated) | ⬜ not supplied — unresolved |
| [10](animations/tangents/23_cat_theological_mechanical_history/clips/10/README.md) | ✅ `cat_10.png` (annotated) | ⬜ not supplied — unresolved |
| [11](animations/tangents/23_cat_theological_mechanical_history/clips/11/README.md) | ✅ `cat_11.png` (annotated) | ⬜ not supplied — unresolved |
| [12](animations/tangents/23_cat_theological_mechanical_history/clips/12/README.md) | ✅ `cat_12.png` (annotated) | ⬜ not supplied — unresolved |

- [ ] Clip 01 END: no declared image
- [ ] Clip 02 END: no declared image
- [ ] Clip 03 END: no declared image
- [ ] Clip 04 END: no declared image
- [ ] Clip 05 END: no declared image
- [ ] Clip 06 END: no declared image
- [ ] Clip 07 END: no declared image
- [ ] Clip 08 END: no declared image
- [ ] Clip 09 END: no declared image
- [ ] Clip 10 END: no declared image
- [ ] Clip 11 END: no declared image
- [ ] Clip 12 END: no declared image

### 34_mv_elevator_incident — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/tangents/34_mv_elevator_incident/clips/01/README.md) | ✅ `elevator_01.png` (annotated) | ⬜ not supplied — unresolved |
| [02](animations/tangents/34_mv_elevator_incident/clips/02/README.md) | ✅ `elevator_02.png` (annotated) | ⬜ not supplied — unresolved |
| [03](animations/tangents/34_mv_elevator_incident/clips/03/README.md) | ✅ `elevator_03.png` (annotated) | ⬜ not supplied — unresolved |
| [04](animations/tangents/34_mv_elevator_incident/clips/04/README.md) | ✅ `elevator_04.png` (annotated) | ⬜ not supplied — unresolved |
| [05](animations/tangents/34_mv_elevator_incident/clips/05/README.md) | ✅ `elevator_05.png` (annotated) | ⬜ not supplied — unresolved |
| [06](animations/tangents/34_mv_elevator_incident/clips/06/README.md) | ✅ `elevator_06.png` (annotated) | ⬜ not supplied — unresolved |
| [07](animations/tangents/34_mv_elevator_incident/clips/07/README.md) | ✅ `elevator_07.png` (annotated) | ⬜ not supplied — unresolved |
| [08](animations/tangents/34_mv_elevator_incident/clips/08/README.md) | ✅ `elevator_08.png` (annotated) | ⬜ not supplied — unresolved |
| [09](animations/tangents/34_mv_elevator_incident/clips/09/README.md) | ✅ `elevator_09.png` (annotated) | ⬜ not supplied — unresolved |
| [10](animations/tangents/34_mv_elevator_incident/clips/10/README.md) | ✅ `elevator_10.png` (annotated) | ⬜ not supplied — unresolved |
| [11](animations/tangents/34_mv_elevator_incident/clips/11/README.md) | ✅ `elevator_11.png` (annotated) | ⬜ not supplied — unresolved |
| [12](animations/tangents/34_mv_elevator_incident/clips/12/README.md) | ✅ `elevator_12.png` (annotated) | ⬜ not supplied — unresolved |

- [ ] Clip 01 END: no declared image
- [ ] Clip 02 END: no declared image
- [ ] Clip 03 END: no declared image
- [ ] Clip 04 END: no declared image
- [ ] Clip 05 END: no declared image
- [ ] Clip 06 END: no declared image
- [ ] Clip 07 END: no declared image
- [ ] Clip 08 END: no declared image
- [ ] Clip 09 END: no declared image
- [ ] Clip 10 END: no declared image
- [ ] Clip 11 END: no declared image
- [ ] Clip 12 END: no declared image

### 02_pkg_install_small_empire_of_dependencies — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/tangents/02_pkg_install_small_empire_of_dependencies/clips/01/README.md) | ✅ `package_01.png` (annotated) | ⬜ not supplied — unresolved |
| [02](animations/tangents/02_pkg_install_small_empire_of_dependencies/clips/02/README.md) | ✅ `package_02.png` (annotated) | ⬜ not supplied — unresolved |
| [03](animations/tangents/02_pkg_install_small_empire_of_dependencies/clips/03/README.md) | ✅ `package_03.png` (annotated) | ⬜ not supplied — unresolved |
| [04](animations/tangents/02_pkg_install_small_empire_of_dependencies/clips/04/README.md) | ✅ `package_04.png` (annotated) | ⬜ not supplied — unresolved |
| [05](animations/tangents/02_pkg_install_small_empire_of_dependencies/clips/05/README.md) | ✅ `package_05.png` (annotated) | ⬜ not supplied — unresolved |
| [06](animations/tangents/02_pkg_install_small_empire_of_dependencies/clips/06/README.md) | ✅ `package_06.png` (annotated) | ⬜ not supplied — unresolved |
| [07](animations/tangents/02_pkg_install_small_empire_of_dependencies/clips/07/README.md) | ✅ `package_07.png` (annotated) | ⬜ not supplied — unresolved |
| [08](animations/tangents/02_pkg_install_small_empire_of_dependencies/clips/08/README.md) | ✅ `package_08.png` (annotated) | ⬜ not supplied — unresolved |
| [09](animations/tangents/02_pkg_install_small_empire_of_dependencies/clips/09/README.md) | ✅ `package_09.png` (annotated) | ⬜ not supplied — unresolved |
| [10](animations/tangents/02_pkg_install_small_empire_of_dependencies/clips/10/README.md) | ✅ `package_10.png` (annotated) | ⬜ not supplied — unresolved |
| [11](animations/tangents/02_pkg_install_small_empire_of_dependencies/clips/11/README.md) | ✅ `package_11.png` (annotated) | ⬜ not supplied — unresolved |
| [12](animations/tangents/02_pkg_install_small_empire_of_dependencies/clips/12/README.md) | ✅ `package_12.png` (annotated) | ⬜ not supplied — unresolved |

- [ ] Clip 01 END: no declared image
- [ ] Clip 02 END: no declared image
- [ ] Clip 03 END: no declared image
- [ ] Clip 04 END: no declared image
- [ ] Clip 05 END: no declared image
- [ ] Clip 06 END: no declared image
- [ ] Clip 07 END: no declared image
- [ ] Clip 08 END: no declared image
- [ ] Clip 09 END: no declared image
- [ ] Clip 10 END: no declared image
- [ ] Clip 11 END: no declared image
- [ ] Clip 12 END: no declared image

### 22_find_cartographical_disaster — incomplete

| Clip | START | END |
| --- | --- | --- |
| [01](animations/tangents/22_find_cartographical_disaster/clips/01/README.md) | ✅ `cartography_01.png` (annotated) | ⬜ not supplied — unresolved |
| [02](animations/tangents/22_find_cartographical_disaster/clips/02/README.md) | ✅ `cartography_02.png` (annotated) | ⬜ not supplied — unresolved |
| [03](animations/tangents/22_find_cartographical_disaster/clips/03/README.md) | ✅ `cartography_03.png` (annotated) | ⬜ not supplied — unresolved |
| [04](animations/tangents/22_find_cartographical_disaster/clips/04/README.md) | ✅ `cartography_04.png` (annotated) | ⬜ not supplied — unresolved |
| [05](animations/tangents/22_find_cartographical_disaster/clips/05/README.md) | ✅ `cartography_05.png` (annotated) | ⬜ not supplied — unresolved |
| [06](animations/tangents/22_find_cartographical_disaster/clips/06/README.md) | ✅ `cartography_06.png` (annotated) | ⬜ not supplied — unresolved |

- [ ] Clip 01 END: no declared image
- [ ] Clip 02 END: no declared image
- [ ] Clip 03 END: no declared image
- [ ] Clip 04 END: no declared image
- [ ] Clip 05 END: no declared image
- [ ] Clip 06 END: no declared image

## Legacy

### ls — both boundaries supplied

| Clip | START | END |
| --- | --- | --- |
| [01](animations/legacy/ls/clips/01/README.md) | ✅ `ls_clip_01_start.png` | ✅ `ls_clip_01_end.png` |
| [02](animations/legacy/ls/clips/02/README.md) | ✅ `ls_clip_02_start.png` | ✅ `ls_clip_02_end.png` |
| [03](animations/legacy/ls/clips/03/README.md) | ✅ `ls_clip_03_start.png` | ✅ `ls_clip_03_end.png` |
| [04](animations/legacy/ls/clips/04/README.md) | ✅ `ls_clip_04_start.png` | ✅ `ls_clip_04_end.png` |

### echo — both boundaries supplied

| Clip | START | END |
| --- | --- | --- |
| [01](animations/legacy/echo/clips/01/README.md) | ✅ `echo_clip_01_start.png` | ✅ `echo_clip_01_end.png` |
| [02](animations/legacy/echo/clips/02/README.md) | ✅ `echo_clip_02_start.png` | ✅ `echo_clip_02_end.png` |
| [03](animations/legacy/echo/clips/03/README.md) | ✅ `echo_clip_03_start.png` | ✅ `echo_clip_03_end.png` |
| [04](animations/legacy/echo/clips/04/README.md) | ✅ `echo_clip_04_start.png` | ✅ `echo_clip_04_end.png` |

### ls_tangent — both boundaries supplied

| Clip | START | END |
| --- | --- | --- |
| [01](animations/legacy/ls_tangent/clips/01/README.md) | ✅ `ls_tangent_clip_01_start.png` | ✅ `ls_tangent_clip_01_end.png` |
| [02](animations/legacy/ls_tangent/clips/02/README.md) | ✅ `ls_tangent_clip_02_start.png` | ✅ `ls_tangent_clip_02_end.png` |
| [03](animations/legacy/ls_tangent/clips/03/README.md) | ✅ `ls_tangent_clip_03_start.png` | ✅ `ls_tangent_clip_03_end.png` |
| [04](animations/legacy/ls_tangent/clips/04/README.md) | ✅ `ls_tangent_clip_04_start.png` | ✅ `ls_tangent_clip_04_end.png` |
| [05](animations/legacy/ls_tangent/clips/05/README.md) | ✅ `ls_tangent_clip_05_start.png` | ✅ `ls_tangent_clip_05_end.png` |
| [06](animations/legacy/ls_tangent/clips/06/README.md) | ✅ `ls_tangent_clip_06_start.png` | ✅ `ls_tangent_clip_06_end.png` |
