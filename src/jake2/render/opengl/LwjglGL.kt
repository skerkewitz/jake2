package jake2.render.opengl

import java.nio.*

import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.*

open class LwjglGL internal constructor()// singleton
    : QGL {

    override fun glAlphaFunc(func: Int, ref: Float) {
        GL11.glAlphaFunc(func, ref)
    }

    override fun glBegin(mode: Int) {
        GL11.glBegin(mode)
    }

    override fun glBindTexture(target: Int, texture: Int) {
        GL11.glBindTexture(target, texture)
    }

    override fun glBlendFunc(sfactor: Int, dfactor: Int) {
        GL11.glBlendFunc(sfactor, dfactor)
    }

    override fun glClear(mask: Int) {
        GL11.glClear(mask)
    }

    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) {
        GL11.glClearColor(red, green, blue, alpha)
    }

    override fun glColor3f(red: Float, green: Float, blue: Float) {
        GL11.glColor3f(red, green, blue)
    }

    override fun glColor3ub(red: Byte, green: Byte, blue: Byte) {
        GL11.glColor3ub(red, green, blue)
    }

    override fun glColor4f(red: Float, green: Float, blue: Float, alpha: Float) {
        GL11.glColor4f(red, green, blue, alpha)
    }

    override fun glColor4ub(red: Byte, green: Byte, blue: Byte, alpha: Byte) {
        GL11.glColor4ub(red, green, blue, alpha)
    }

    override fun glColorPointer(size: Int, unsigned: Boolean, stride: Int, pointer: ByteBuffer) {
        GL11.glColorPointer(size, GL11.GL_BYTE, stride, pointer)
    }

    override fun glColorPointer(size: Int, stride: Int, pointer: FloatBuffer) {
        GL11.glColorPointer(size, GL11.GL_FLOAT, stride, pointer)
    }

    override fun glCullFace(mode: Int) {
        GL11.glCullFace(mode)
    }

    override fun glDeleteTextures(textures: IntBuffer) {
        GL11.glDeleteTextures(textures)
    }

    override fun glDepthFunc(func: Int) {
        GL11.glDepthFunc(func)
    }

    override fun glDepthMask(flag: Boolean) {
        GL11.glDepthMask(flag)
    }

    override fun glDepthRange(zNear: Double, zFar: Double) {
        GL11.glDepthRange(zNear, zFar)
    }

    override fun glDisable(cap: Int) {
        GL11.glDisable(cap)
    }

    override fun glDisableClientState(cap: Int) {
        GL11.glDisableClientState(cap)
    }

    override fun glDrawArrays(mode: Int, first: Int, count: Int) {
        GL11.glDrawArrays(mode, first, count)
    }

    override fun glDrawBuffer(mode: Int) {
        GL11.glDrawBuffer(mode)
    }

    override fun glDrawElements(mode: Int, indices: IntBuffer) {
        GL11.glDrawElements(mode, indices)
    }

    override fun glEnable(cap: Int) {
        GL11.glEnable(cap)
    }

    override fun glEnableClientState(cap: Int) {
        GL11.glEnableClientState(cap)
    }

    override fun glEnd() {
        GL11.glEnd()
    }

    override fun glFinish() {
        GL11.glFinish()
    }

    override fun glFlush() {
        GL11.glFlush()
    }

    override fun glFrustum(left: Double, right: Double, bottom: Double,
                           top: Double, zNear: Double, zFar: Double) {
        GL11.glFrustum(left, right, bottom, top, zNear, zFar)
    }

    override fun glGetError(): Int {
        return GL11.glGetError()
    }

    override fun glGetFloat(pname: Int, params: FloatBuffer) {
        GL11.glGetFloatv(pname, params)
    }

    override fun glGetString(name: Int): String {
        return GL11.glGetString(name)
    }

    override fun glHint(target: Int, mode: Int) {
        GL11.glHint(target, mode)
    }

    override fun glInterleavedArrays(format: Int, stride: Int,
                                     pointer: FloatBuffer) {
        GL11.glInterleavedArrays(format, stride, pointer)
    }

    override fun glLoadIdentity() {
        GL11.glLoadIdentity()
    }

    override fun glLoadMatrix(m: FloatBuffer) {
        GL11.glLoadMatrixf(m)
    }

    override fun glMatrixMode(mode: Int) {
        GL11.glMatrixMode(mode)
    }

    override fun glOrtho(left: Double, right: Double, bottom: Double,
                         top: Double, zNear: Double, zFar: Double) {
        GL11.glOrtho(left, right, bottom, top, zNear, zFar)
    }

    override fun glPixelStorei(pname: Int, param: Int) {
        GL11.glPixelStorei(pname, param)
    }

    override fun glPointSize(size: Float) {
        GL11.glPointSize(size)
    }

    override fun glPolygonMode(face: Int, mode: Int) {
        GL11.glPolygonMode(face, mode)
    }

    override fun glPopMatrix() {
        GL11.glPopMatrix()
    }

    override fun glPushMatrix() {
        GL11.glPushMatrix()
    }

    override fun glReadPixels(x: Int, y: Int, width: Int, height: Int,
                              format: Int, type: Int, pixels: ByteBuffer) {
        GL11.glReadPixels(x, y, width, height, format, type, pixels)
    }

    override fun glRotatef(angle: Float, x: Float, y: Float, z: Float) {
        GL11.glRotatef(angle, x, y, z)
    }

    override fun glScalef(x: Float, y: Float, z: Float) {
        GL11.glScalef(x, y, z)
    }

    override fun glScissor(x: Int, y: Int, width: Int, height: Int) {
        GL11.glScissor(x, y, width, height)
    }

    override fun glShadeModel(mode: Int) {
        GL11.glShadeModel(mode)
    }

    override fun glTexCoord2f(s: Float, t: Float) {
        GL11.glTexCoord2f(s, t)
    }

    override fun glTexCoordPointer(size: Int, stride: Int, pointer: FloatBuffer) {
        GL11.glTexCoordPointer(size, GL11.GL_FLOAT, stride, pointer)
    }

    override fun glTexEnvi(target: Int, pname: Int, param: Int) {
        GL11.glTexEnvi(target, pname, param)
    }

    override fun glTexImage2D(target: Int, level: Int, internalformat: Int,
                              width: Int, height: Int, border: Int, format: Int, type: Int,
                              pixels: ByteBuffer) {
        GL11.glTexImage2D(target, level, internalformat, width, height, border,
                format, type, pixels)
    }

    override fun glTexImage2D(target: Int, level: Int, internalformat: Int,
                              width: Int, height: Int, border: Int, format: Int, type: Int,
                              pixels: IntBuffer) {
        GL11.glTexImage2D(target, level, internalformat, width, height, border,
                format, type, pixels)
    }

    override fun glTexParameterf(target: Int, pname: Int, param: Float) {
        GL11.glTexParameterf(target, pname, param)
    }

    override fun glTexParameteri(target: Int, pname: Int, param: Int) {
        GL11.glTexParameteri(target, pname, param)
    }

    override fun glTexSubImage2D(target: Int, level: Int, xoffset: Int,
                                 yoffset: Int, width: Int, height: Int, format: Int, type: Int,
                                 pixels: IntBuffer) {
        GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height,
                format, type, pixels)
    }

    override fun glTranslatef(x: Float, y: Float, z: Float) {
        GL11.glTranslatef(x, y, z)
    }

    override fun glVertex2f(x: Float, y: Float) {
        GL11.glVertex2f(x, y)
    }

    override fun glVertex3f(x: Float, y: Float, z: Float) {
        GL11.glVertex3f(x, y, z)
    }

    override fun glVertexPointer(size: Int, stride: Int, pointer: FloatBuffer) {
        GL11.glVertexPointer(size, GL11.GL_FLOAT, stride, pointer)
    }

    override fun glViewport(x: Int, y: Int, width: Int, height: Int) {
        GL11.glViewport(x, y, width, height)
    }

    override fun glColorTable(target: Int, internalFormat: Int, width: Int,
                              format: Int, type: Int, data: ByteBuffer) {
        //        EXTPalettedTexture.glColorTableEXT(target, internalFormat, width, format, type, data);
    }

    override fun glActiveTextureARB(texture: Int) {
        ARBMultitexture.glActiveTextureARB(texture)
    }

    override fun glClientActiveTextureARB(texture: Int) {
        ARBMultitexture.glClientActiveTextureARB(texture)
    }

    override fun glPointParameterEXT(pname: Int, pfParams: FloatBuffer) {
        //        EXTPointParameters.glPointParameterEXT(pname, pfParams);
    }

    override fun glPointParameterfEXT(pname: Int, param: Float) {
        EXTPointParameters.glPointParameterfEXT(pname, param)
    }

    override fun glLockArraysEXT(first: Int, count: Int) {
        EXTCompiledVertexArray.glLockArraysEXT(first, count)
    }

    override fun glArrayElement(index: Int) {
        GL11.glArrayElement(index)
    }

    override fun glUnlockArraysEXT() {
        EXTCompiledVertexArray.glUnlockArraysEXT()
    }

    override fun glMultiTexCoord2f(target: Int, s: Float, t: Float) {
        GL13.glMultiTexCoord2f(target, s, t)
    }

    /*
     * util extensions
     */
    override fun setSwapInterval(interval: Int) {
        GLFW.glfwSwapInterval(interval)
    }

}
