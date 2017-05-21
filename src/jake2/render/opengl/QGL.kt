package jake2.render.opengl

import java.nio.*

interface QGL : QGLConst {

    /*
     * a sub set of OpenGL for Jake2
     */

    fun glActiveTextureARB(texture: Int)

    fun glAlphaFunc(func: Int, ref: Float)

    fun glArrayElement(index: Int)

    fun glBegin(mode: Int)

    fun glBindTexture(target: Int, texture: Int)

    fun glBlendFunc(sfactor: Int, dfactor: Int)

    fun glClear(mask: Int)

    fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float)

    fun glClientActiveTextureARB(texture: Int)

    fun glColor3f(red: Float, green: Float, blue: Float)

    fun glColor3ub(red: Byte, green: Byte, blue: Byte)

    fun glColor4f(red: Float, green: Float, blue: Float, alpha: Float)

    fun glColor4ub(red: Byte, green: Byte, blue: Byte, alpha: Byte)

    fun glColorPointer(size: Int, unsigned: Boolean, stride: Int,
                       pointer: ByteBuffer)

    fun glColorPointer(size: Int, stride: Int, pointer: FloatBuffer)

    fun glColorTable(target: Int, internalFormat: Int, width: Int, format: Int,
                     type: Int, data: ByteBuffer)

    fun glCullFace(mode: Int)

    fun glDeleteTextures(textures: IntBuffer)

    fun glDepthFunc(func: Int)

    fun glDepthMask(flag: Boolean)

    fun glDepthRange(zNear: Double, zFar: Double)

    fun glDisable(cap: Int)

    fun glDisableClientState(cap: Int)

    fun glDrawArrays(mode: Int, first: Int, count: Int)

    fun glDrawBuffer(mode: Int)

    fun glDrawElements(mode: Int, indices: IntBuffer)

    fun glEnable(cap: Int)

    fun glEnableClientState(cap: Int)

    fun glEnd()

    fun glFinish()

    fun glFlush()

    fun glFrustum(left: Double, right: Double, bottom: Double, top: Double,
                  zNear: Double, zFar: Double)

    fun glGetError(): Int

    fun glGetFloat(pname: Int, params: FloatBuffer)

    fun glGetString(name: Int): String

    fun glHint(target: Int, mode: Int)

    fun glInterleavedArrays(format: Int, stride: Int, pointer: FloatBuffer)

    fun glLockArraysEXT(first: Int, count: Int)

    fun glLoadIdentity()

    fun glLoadMatrix(m: FloatBuffer)

    fun glMatrixMode(mode: Int)

    fun glMultiTexCoord2f(target: Int, s: Float, t: Float)

    fun glOrtho(left: Double, right: Double, bottom: Double, top: Double,
                zNear: Double, zFar: Double)

    fun glPixelStorei(pname: Int, param: Int)

    fun glPointParameterEXT(pname: Int, pfParams: FloatBuffer)

    fun glPointParameterfEXT(pname: Int, param: Float)

    fun glPointSize(size: Float)

    fun glPolygonMode(face: Int, mode: Int)

    fun glPopMatrix()

    fun glPushMatrix()

    fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int,
                     type: Int, pixels: ByteBuffer)

    fun glRotatef(angle: Float, x: Float, y: Float, z: Float)

    fun glScalef(x: Float, y: Float, z: Float)

    fun glScissor(x: Int, y: Int, width: Int, height: Int)

    fun glShadeModel(mode: Int)

    fun glTexCoord2f(s: Float, t: Float)

    fun glTexCoordPointer(size: Int, stride: Int, pointer: FloatBuffer)

    fun glTexEnvi(target: Int, pname: Int, param: Int)

    fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int,
                     height: Int, border: Int, format: Int, type: Int, pixels: ByteBuffer)

    fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int,
                     height: Int, border: Int, format: Int, type: Int, pixels: IntBuffer)

    fun glTexParameterf(target: Int, pname: Int, param: Float)

    fun glTexParameteri(target: Int, pname: Int, param: Int)

    fun glTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int,
                        width: Int, height: Int, format: Int, type: Int, pixels: IntBuffer)

    fun glTranslatef(x: Float, y: Float, z: Float)

    fun glUnlockArraysEXT()

    fun glVertex2f(x: Float, y: Float)

    fun glVertex3f(x: Float, y: Float, z: Float)

    fun glVertexPointer(size: Int, stride: Int, pointer: FloatBuffer)

    fun glViewport(x: Int, y: Int, width: Int, height: Int)

    /*
     * util extensions
     */
    fun setSwapInterval(interval: Int)

}