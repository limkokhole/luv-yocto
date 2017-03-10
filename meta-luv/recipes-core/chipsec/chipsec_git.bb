SUMMARY = "CHIPSEC: Platform Security Assessment Framework"
DESCRIPTION = "CHIPSEC is a framework for analyzing security of PC \
platforms including hardware, system firmware including BIOS/UEFI \
and the configuration of platform components."

LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://LICENSE;md5=8c16666ae6c159876a0ba63099614381"

SRC_URI = "git://github.com/chipsec/chipsec.git \
    file://0001-drivers-linux-Do-not-host-system-s-kernel-source-dir.patch \
    file://chipsec file://luv-parser-chipsec \
    file://fix-setup.py-for-Linux.patch \
    file://chipsec-setup-install-cores-library-under-helper-lin.patch \
    file://0001-chipsec-building-for-32-bit-systems.patch \
    file://0002-chipsec_km-utilize-inode_lock-unlock-wrappers-for-ne.patch"

SRCREV="20cc5a30675548a764dadfe0dc677a283816906c"
PV="1.2.2"

DEPENDS = "virtual/kernel python-core nasm-native python-setuptools-native"
RDEPENDS_${PN} = "python python-shell python-stringold python-xml \
    python-ctypes python-fcntl python-importlib python-json python-mmap \
    python-resource"

COMPATIBLE_HOST='(i.86|x86_64).*'

inherit module-base
inherit python-dir
inherit distutils
inherit luv-test

S = "${WORKDIR}/git"

export INC = "-I${STAGING_INCDIR}/${PYTHON_DIR}"

def get_target_arch(d):
 import re
 target = d.getVar('TARGET_ARCH', True)
 if target == "x86_64":
    return 'x86_64'
 elif re.match('i.86', target):
    return 'i386'
 elif re.match('arm', target):
    return 'arm'
 else:
    raise bb.parse.SkipPackage("TARGET_ARCH %s not supported!" % target)

EXTRA_OEMAKE += "ARCH="${@get_target_arch(d)}""

DISTUTILS_INSTALL_ARGS = "--root=${D}${PYTHON_SITEPACKAGES_DIR} \
    --install-data=${D}/${datadir}"

fix_mod_path() {
    sed -i -e "s:^INSTALL_MOD_PATH_PREFIX = .*:INSTALL_MOD_PATH_PREFIX = \"${PYTHON_SITEPACKAGES_DIR}\":" ${S}/source/tool/chipsec_main.py
    sed -i -e "s:PYTHONPATH:${PYTHON_SITEPACKAGES_DIR}:" ${WORKDIR}/chipsec
}

fix_kernel_source_dir() {
    sed -i "s:LUV_KERNEL_SRC_DIR:${STAGING_KERNEL_DIR}:" ${S}/drivers/linux/Makefile
}

do_patch_append() {
    bb.build.exec_func('fix_mod_path', d)
    bb.build.exec_func('fix_kernel_source_dir', d)
}

do_install_append() {
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/chipsec ${D}${bindir}

    #
    # FIXME: for some reason chipsec ends up installed in a repeated
    # directory structure. Thus, we need to move it to its proper location
    # under PYTHON_SITEPACKAGES_DIR
    #

    install -d ${D}${PYTHON_SITEPACKAGES_DIR}/${PN}
    mv ${D}${PYTHON_SITEPACKAGES_DIR}${D}${PYTHON_SITEPACKAGES_DIR}/* ${D}${PYTHON_SITEPACKAGES_DIR}/${PN}
    # remove old files
    cd ${D}${PYTHON_SITEPACKAGES_DIR}
    ls | grep -v chipsec | xargs rm -fr
    cd $OLDPWD
}

LUV_TEST_LOG_PARSER="luv-parser-chipsec"

FILES_${PN}-dbg +="${libdir}/${PYTHON_DIR}/site-packages/${PN}/helper/linux/.debug"
